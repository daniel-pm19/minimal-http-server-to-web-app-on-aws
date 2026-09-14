package com.escuelaing.httpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * A sequential HTTP server: one accept loop, one connection handled fully
 * before the next is accepted. No thread pool, no per-connection thread -
 * concurrency is explicitly out of scope for this lab, so a request that
 * takes a long time (see the /slow demo endpoint) blocks every other client.
 */
public final class HttpServer {

    /**
     * Max time a connection may sit idle waiting for a request line before it
     * is dropped. Without this, a client that connects and never sends
     * anything (an idle health-checker, a port scanner, or a slow-loris
     * style probe) would block reader.readLine() forever - and because this
     * server is sequential, that single stuck connection would freeze every
     * other client indefinitely, not just its own request.
     */
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 10_000;

    private final int port;
    private final int readTimeoutMillis;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public HttpServer(int port) {
        this(port, DEFAULT_READ_TIMEOUT_MILLIS);
    }

    /** @param readTimeoutMillis exposed mainly so tests can use a short timeout instead of waiting 10s. */
    public HttpServer(int port, int readTimeoutMillis) {
        this.port = port;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    /** Actual bound port - useful when constructed with 0 (ephemeral port) in tests. */
    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    /** Binds the listening socket. Split from acceptLoop() so tests can bind
     *  synchronously, read back the ephemeral port, and only then start
     *  accepting connections on a background thread. */
    public void bind() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        log("Listening on port " + getPort());
    }

    public void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleConnection(clientSocket);
            } catch (IOException e) {
                if (running) {
                    log("Connection error: " + e.getMessage());
                }
                // otherwise stop() closed the socket on purpose; exit quietly.
            }
        }
    }

    public void start() throws IOException {
        bind();
        acceptLoop();
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    private void handleConnection(Socket clientSocket) {
        try (clientSocket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.US_ASCII));
             OutputStream out = clientSocket.getOutputStream()) {

            clientSocket.setSoTimeout(readTimeoutMillis);
            HttpRequest request = HttpRequest.parse(in);
            log(request.getMethod() + " " + request.getPath());
            HttpResponse response = Router.route(request);
            response.writeTo(out);

        } catch (MalformedRequestException e) {
            log("Malformed request ignored: " + e.getMessage());
        } catch (IOException e) {
            log("I/O error handling connection: " + e.getMessage());
        }
    }

    private static void log(String message) {
        System.out.println("[" + Instant.now() + "] " + message);
    }

    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        new HttpServer(port).start();
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        }
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            return Integer.parseInt(envPort);
        }
        return 8080;
    }
}
