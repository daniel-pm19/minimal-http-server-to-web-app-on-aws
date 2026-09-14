package com.escuelaing.httpserver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the real server on an ephemeral port and drives it with real HTTP
 * requests over java.net.http.HttpClient - no mocks, no sockets stubbed out.
 *
 * java.net.http.HttpRequest and java.net.http.HttpResponse clash by simple
 * name with this package's own HttpRequest/HttpResponse, so they are always
 * referenced fully-qualified below instead of imported.
 */
class HttpServerIntegrationTest {

    private static HttpServer server;
    private static String baseUrl;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws IOException {
        server = new HttpServer(0);
        server.bind(); // synchronous: getPort() below is safe immediately after this returns
        Thread serverThread = new Thread(server::acceptLoop, "test-http-server");
        serverThread.setDaemon(true);
        serverThread.start();
        baseUrl = "http://localhost:" + server.getPort();
    }

    @AfterAll
    static void stopServer() throws IOException {
        server.stop();
    }

    private static java.net.http.HttpResponse<String> getText(String path) throws Exception {
        java.net.http.HttpRequest request =
                java.net.http.HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        return CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private static java.net.http.HttpResponse<byte[]> getBytes(String path) throws Exception {
        java.net.http.HttpRequest request =
                java.net.http.HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        return CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test
    void homePageIsServed() throws Exception {
        var response = getText("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(response.body().contains("<html"));
    }

    @Test
    void javascriptIsServedWithCorrectType() throws Exception {
        var response = getText("/app.js");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("javascript"));
    }

    @Test
    void pngImageIsTransferredByteForByte() throws Exception {
        var response = getBytes("/logo.png");
        assertEquals(200, response.statusCode());
        assertEquals("image/png", response.headers().firstValue("Content-Type").orElse(""));
        byte[] expected = getClass().getResourceAsStream("/public/logo.png").readAllBytes();
        assertArrayEquals(expected, response.body());
    }

    @Test
    void jpegImageIsTransferredByteForByte() throws Exception {
        var response = getBytes("/photo.jpg");
        assertEquals(200, response.statusCode());
        assertEquals("image/jpeg", response.headers().firstValue("Content-Type").orElse(""));
        byte[] expected = getClass().getResourceAsStream("/public/photo.jpg").readAllBytes();
        assertArrayEquals(expected, response.body());
    }

    @Test
    void missingResourceReturns404() throws Exception {
        var response = getText("/does-not-exist.html");
        assertEquals(404, response.statusCode());
    }

    @Test
    void pathTraversalIsRejectedWith400() throws Exception {
        java.net.http.HttpRequest request =
                java.net.http.HttpRequest.newBuilder(URI.create(baseUrl + "/../pom.xml")).GET().build();
        var response = CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(URI.create(baseUrl + "/"))
                .method("POST", java.net.http.HttpRequest.BodyPublishers.noBody())
                .build();
        var response = CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
        assertEquals("GET", response.headers().firstValue("Allow").orElse(""));
    }

    @Test
    void greetingServiceReturnsJson() throws Exception {
        var response = getText("/greeting?name=Daniel");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        assertEquals("{\"message\":\"Hello, Daniel!\"}", response.body());
    }

    @Test
    void greetingServiceRejectsMissingName() throws Exception {
        var response = getText("/greeting");
        assertEquals(400, response.statusCode());
    }

    @Test
    void squareServiceComputesSquare() throws Exception {
        var response = getText("/square?number=6");
        assertEquals(200, response.statusCode());
        assertEquals("{\"input\":6,\"square\":36}", response.body());
    }

    @Test
    void squareServiceRejectsInvalidNumber() throws Exception {
        var response = getText("/square?number=notanumber");
        assertEquals(400, response.statusCode());
    }

    @Test
    void healthServiceReportsUp() throws Exception {
        var response = getText("/health");
        assertEquals(200, response.statusCode());
        assertEquals("{\"status\":\"UP\"}", response.body());
    }

    @Test
    void timeServiceReturnsAServerTimestamp() throws Exception {
        var response = getText("/time");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("serverTime"));
    }

    @Test
    void tenConsecutiveRequestsAllSucceed() throws Exception {
        for (int i = 0; i < 10; i++) {
            var response = getText("/health");
            assertEquals(200, response.statusCode());
        }
    }

    @Test
    void malformedRequestDoesNotCrashTheServer() throws Exception {
        // Send bytes that don't form a valid request line ("GARBAGE" has no
        // method/path split), then close without ever reading a response -
        // HttpRequest.parse() should throw MalformedRequestException, which
        // HttpServer.handleConnection() must catch so only this one
        // connection is dropped, not the whole accept loop.
        String hostname = URI.create(baseUrl).getHost();
        int port = URI.create(baseUrl).getPort();
        try (Socket raw = new Socket(hostname, port)) {
            OutputStream out = raw.getOutputStream();
            out.write("GARBAGE\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }

        // The server must still be alive and answering normal requests afterward.
        var response = getText("/health");
        assertEquals(200, response.statusCode());
        assertEquals("{\"status\":\"UP\"}", response.body());
    }

    @Test
    void serverIsSequentialASlowRequestBlocksTheNextOne() throws Exception {
        // Fire /slow in the background, then - after giving it a head start to
        // be the one actually accept()-ed first - fire /health from this
        // thread. If the server were concurrent, /health would return almost
        // immediately; because the accept loop handles one connection fully
        // before accepting the next, /health must wait behind /slow's 5s
        // sleep. This is real, measured evidence for report section 6.2.
        long start = System.nanoTime();
        CompletableFuture<java.net.http.HttpResponse<String>> slowCall = CompletableFuture.supplyAsync(() -> {
            try {
                return getText("/slow");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(300);
        var healthResponse = getText("/health");
        long healthElapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertEquals(200, healthResponse.statusCode());
        assertTrue(healthElapsedMs >= 4000,
                "Expected /health to wait behind /slow (sequential server), but it returned after only "
                        + healthElapsedMs + " ms");

        assertEquals(200, slowCall.get().statusCode());
    }

    @Test
    void idleConnectionDoesNotBlockOtherRequestsForever() throws Exception {
        // A client that connects and never sends a request line must not
        // freeze the whole (sequential) server. Uses its own server instance
        // with a short read timeout so the test doesn't have to wait out the
        // production 10s default.
        HttpServer shortTimeoutServer = new HttpServer(0, 300);
        shortTimeoutServer.bind();
        Thread thread = new Thread(shortTimeoutServer::acceptLoop, "idle-timeout-test-server");
        thread.setDaemon(true);
        thread.start();
        String url = "http://localhost:" + shortTimeoutServer.getPort();

        try (Socket idleSocket = new Socket("localhost", shortTimeoutServer.getPort())) {
            Thread.sleep(50); // let the accept loop actually pick up this connection first

            long start = System.nanoTime();
            java.net.http.HttpRequest request =
                    java.net.http.HttpRequest.newBuilder(URI.create(url + "/health")).GET().build();
            var response = CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

            assertEquals(200, response.statusCode());
            assertTrue(elapsedMs < 2000,
                    "Expected the idle connection's read timeout to release the server quickly, but /health took "
                            + elapsedMs + " ms");
        } finally {
            shortTimeoutServer.stop();
        }
    }
}
