package com.escuelaing.httpserver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Pure, allocation-only path normalizer used to keep static-file requests
 * inside the public resources area. It resolves "." and ".." segments
 * itself instead of trusting java.nio.file.Path, because the resources are
 * served from the classpath (possibly inside a jar), where there is no real
 * filesystem path to canonicalize against.
 */
public final class PathSanitizer {

    private PathSanitizer() {
    }

    /**
     * @param rawPath an already percent-decoded request path
     * @return the normalized, always-rooted path (e.g. "/images/logo.png"),
     *         or empty if the path tries to climb above the root.
     */
    public static Optional<String> sanitize(String rawPath) {
        if (rawPath == null || rawPath.isEmpty() || rawPath.indexOf('\0') >= 0) {
            return Optional.empty();
        }

        String normalizedSlashes = rawPath.replace('\\', '/');
        Deque<String> segments = new ArrayDeque<>();

        for (String segment : normalizedSlashes.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (segments.isEmpty()) {
                    return Optional.empty(); // tries to escape the resource root
                }
                segments.removeLast();
                continue;
            }
            segments.addLast(segment);
        }

        return Optional.of("/" + String.join("/", segments));
    }
}
