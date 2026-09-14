package com.escuelaing.httpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A parsed GET request: method, decoded path, and decoded query parameters.
 * Headers are drained but not kept - this lab never needs to read them back.
 */
public final class HttpRequest {

    private final String method;
    private final String path;
    private final Map<String, String> queryParams;

    private HttpRequest(String method, String path, Map<String, String> queryParams) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public static HttpRequest parse(BufferedReader reader) throws IOException {
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            throw new MalformedRequestException("Empty request line");
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            throw new MalformedRequestException("Malformed request line: " + requestLine);
        }
        String method = parts[0];
        String target = parts[1];

        // Headers aren't used by this lab, but must be drained so the socket
        // is left in a clean state for the response.
        String header;
        while ((header = reader.readLine()) != null && !header.isEmpty()) {
            // intentionally ignored
        }

        String rawPath = target;
        String rawQuery = null;
        int queryIndex = target.indexOf('?');
        if (queryIndex >= 0) {
            rawPath = target.substring(0, queryIndex);
            rawQuery = target.substring(queryIndex + 1);
        }

        String path = decodePath(rawPath);
        Map<String, String> queryParams = parseQuery(rawQuery);
        return new HttpRequest(method, path, queryParams);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String rawKey = eq >= 0 ? pair.substring(0, eq) : pair;
            String rawValue = eq >= 0 ? pair.substring(eq + 1) : "";
            params.put(URLDecoder.decode(rawKey, StandardCharsets.UTF_8),
                    URLDecoder.decode(rawValue, StandardCharsets.UTF_8));
        }
        return params;
    }

    /**
     * Percent-decodes a URL path without treating '+' as a space - that
     * substitution only applies to form/query encoding, not path segments.
     * Doing this before routing is what lets us catch encoded traversal
     * attempts like "%2e%2e%2f" instead of only literal "../".
     */
    private static String decodePath(String rawPath) {
        StringBuilder decoded = new StringBuilder();
        int i = 0;
        while (i < rawPath.length()) {
            char c = rawPath.charAt(i);
            if (c == '%' && i + 2 < rawPath.length()) {
                try {
                    int value = Integer.parseInt(rawPath.substring(i + 1, i + 3), 16);
                    decoded.append((char) value);
                    i += 3;
                    continue;
                } catch (NumberFormatException ignored) {
                    // Not a valid escape sequence; keep the '%' literally.
                }
            }
            decoded.append(c);
            i++;
        }
        return decoded.toString();
    }
}
