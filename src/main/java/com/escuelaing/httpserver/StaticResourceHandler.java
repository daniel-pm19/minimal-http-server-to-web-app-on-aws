package com.escuelaing.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Serves files bundled under src/main/resources/public via the classpath,
 * so the exact same lookup works whether the app runs from the IDE or from
 * the packaged jar deployed to EC2 - there is only one artifact to ship.
 */
public final class StaticResourceHandler {

    private static final String RESOURCE_ROOT = "/public";

    private StaticResourceHandler() {
    }

    public static HttpResponse serve(String requestPath) {
        String effectivePath = requestPath.equals("/") ? "/index.html" : requestPath;

        Optional<String> sanitized = PathSanitizer.sanitize(effectivePath);
        if (sanitized.isEmpty()) {
            return HttpResponse.badRequest("400 Bad Request: unsafe path");
        }

        String classpathLocation = RESOURCE_ROOT + sanitized.get();
        try (InputStream in = StaticResourceHandler.class.getResourceAsStream(classpathLocation)) {
            if (in == null) {
                return HttpResponse.notFound();
            }
            byte[] bytes = in.readAllBytes();
            String contentType = ContentTypes.resolve(classpathLocation);
            return HttpResponse.ok(contentType, bytes);
        } catch (IOException e) {
            return HttpResponse.notFound();
        }
    }
}
