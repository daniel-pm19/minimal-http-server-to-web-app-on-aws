package com.escuelaing.httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An HTTP/1.1 response written as raw bytes. The old prototype used a
 * PrintWriter for every response, which re-encodes everything as text and
 * corrupts binary bodies (images); this class always writes the body as the
 * exact bytes it was given and computes Content-Length from that byte count.
 */
public final class HttpResponse {

    private final int statusCode;
    private final String reasonPhrase;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final byte[] body;

    private HttpResponse(int statusCode, String reasonPhrase, String contentType, byte[] body) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.body = body;
        headers.put("Content-Type", contentType);
        headers.put("Content-Length", String.valueOf(body.length));
        headers.put("Connection", "close");
    }

    public static HttpResponse ok(String contentType, byte[] body) {
        return new HttpResponse(200, "OK", contentType, body);
    }

    public static HttpResponse badRequest(String message) {
        return new HttpResponse(400, "Bad Request", "text/plain; charset=UTF-8",
                message.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse badRequestJson(String jsonBody) {
        return new HttpResponse(400, "Bad Request", "application/json; charset=UTF-8",
                jsonBody.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse notFound() {
        return new HttpResponse(404, "Not Found", "text/plain; charset=UTF-8",
                "404 Not Found".getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse methodNotAllowed(String allowedMethods) {
        HttpResponse response = new HttpResponse(405, "Method Not Allowed", "text/plain; charset=UTF-8",
                "405 Method Not Allowed".getBytes(StandardCharsets.UTF_8));
        response.headers.put("Allow", allowedMethods);
        return response;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public byte[] getBody() {
        return body;
    }

    public void writeTo(OutputStream out) throws IOException {
        StringBuilder head = new StringBuilder();
        head.append("HTTP/1.1 ").append(statusCode).append(' ').append(reasonPhrase).append("\r\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            head.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        head.append("\r\n");

        out.write(head.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }
}
