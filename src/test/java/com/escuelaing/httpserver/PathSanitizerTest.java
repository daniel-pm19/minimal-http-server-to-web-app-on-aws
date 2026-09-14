package com.escuelaing.httpserver;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathSanitizerTest {

    @Test
    void keepsSimplePath() {
        assertEquals(Optional.of("/index.html"), PathSanitizer.sanitize("/index.html"));
    }

    @Test
    void keepsNestedPath() {
        assertEquals(Optional.of("/images/logo.png"), PathSanitizer.sanitize("/images/logo.png"));
    }

    @Test
    void collapsesDotSegments() {
        assertEquals(Optional.of("/index.html"), PathSanitizer.sanitize("/./index.html"));
    }

    @Test
    void collapsesRepeatedSlashes() {
        assertEquals(Optional.of("/index.html"), PathSanitizer.sanitize("//index.html"));
    }

    @Test
    void resolvesInternalDotDotWithoutEscaping() {
        assertEquals(Optional.of("/index.html"), PathSanitizer.sanitize("/images/../index.html"));
    }

    @Test
    void rejectsEscapingTheRoot() {
        assertTrue(PathSanitizer.sanitize("/../secret.txt").isEmpty());
    }

    @Test
    void rejectsDeepEscapeAttempt() {
        assertTrue(PathSanitizer.sanitize("/../../../etc/passwd").isEmpty());
    }

    @Test
    void rejectsNullByte() {
        assertTrue(PathSanitizer.sanitize("/index.html\0.png").isEmpty());
    }

    @Test
    void rejectsEmptyPath() {
        assertTrue(PathSanitizer.sanitize("").isEmpty());
    }

    @Test
    void normalizesBackslashes() {
        assertEquals(Optional.of("/images/logo.png"), PathSanitizer.sanitize("\\images\\logo.png"));
    }
}
