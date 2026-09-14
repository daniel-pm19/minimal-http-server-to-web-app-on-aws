package com.escuelaing.httpserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentTypesTest {

    @Test
    void resolvesHtml() {
        assertEquals("text/html; charset=UTF-8", ContentTypes.resolve("/index.html"));
    }

    @Test
    void resolvesJavaScript() {
        assertEquals("text/javascript; charset=UTF-8", ContentTypes.resolve("/app.js"));
    }

    @Test
    void resolvesPng() {
        assertEquals("image/png", ContentTypes.resolve("/logo.png"));
    }

    @Test
    void resolvesBothJpegExtensions() {
        assertEquals("image/jpeg", ContentTypes.resolve("/photo.jpg"));
        assertEquals("image/jpeg", ContentTypes.resolve("/photo.jpeg"));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals("image/png", ContentTypes.resolve("/LOGO.PNG"));
    }

    @Test
    void fallsBackToOctetStreamForUnknownExtension() {
        assertEquals(ContentTypes.DEFAULT, ContentTypes.resolve("/data.bin"));
    }

    @Test
    void fallsBackToOctetStreamWithNoExtension() {
        assertEquals(ContentTypes.DEFAULT, ContentTypes.resolve("/README"));
    }
}
