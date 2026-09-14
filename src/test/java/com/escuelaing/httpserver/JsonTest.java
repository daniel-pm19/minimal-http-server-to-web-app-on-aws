package com.escuelaing.httpserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JsonTest {

    @Test
    void escapesQuotesAndBackslashes() {
        assertEquals("say \\\"hi\\\" \\\\ ok", Json.escape("say \"hi\" \\ ok"));
    }

    @Test
    void escapesControlCharacters() {
        assertEquals("line1\\nline2", Json.escape("line1\nline2"));
    }

    @Test
    void leavesPlainTextUnchanged() {
        assertEquals("Daniel", Json.escape("Daniel"));
    }

    @Test
    void nullBecomesEmptyString() {
        assertEquals("", Json.escape(null));
    }

    @Test
    void escapedValueCannotBreakOutOfTheEnclosingString() {
        String attempt = "\", \"injected\":\"true";
        String json = "{\"message\":\"" + Json.escape(attempt) + "\"}";
        assertFalse(json.contains("\"injected\":\"true\""));
    }
}
