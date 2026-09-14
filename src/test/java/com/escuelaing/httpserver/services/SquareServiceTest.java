package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SquareServiceTest {

    @Test
    void squaresAnInteger() {
        HttpResponse response = SquareService.handle(Map.of("number", "5"));
        assertEquals(200, response.getStatusCode());
        assertEquals("{\"input\":5,\"square\":25}", bodyOf(response));
    }

    @Test
    void squaresADecimal() {
        HttpResponse response = SquareService.handle(Map.of("number", "2.5"));
        assertEquals("{\"input\":2.5,\"square\":6.25}", bodyOf(response));
    }

    @Test
    void squaresANegativeNumber() {
        HttpResponse response = SquareService.handle(Map.of("number", "-3"));
        assertEquals("{\"input\":-3,\"square\":9}", bodyOf(response));
    }

    @Test
    void rejectsMissingNumber() {
        HttpResponse response = SquareService.handle(Map.of());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void rejectsNonNumericInput() {
        HttpResponse response = SquareService.handle(Map.of("number", "abc"));
        assertEquals(400, response.getStatusCode());
        assertTrue(bodyOf(response).contains("not a valid number"));
    }

    @Test
    void rejectsNanEvenThoughJavaParsesItAsANumber() {
        // Double.parseDouble("NaN") succeeds in Java, but a bare (unquoted)
        // NaN token is not valid JSON - this must be rejected, not
        // serialized. The error message is allowed to mention "NaN" as a
        // quoted string; what must never appear is an unquoted ":NaN" value.
        HttpResponse response = SquareService.handle(Map.of("number", "NaN"));
        assertEquals(400, response.getStatusCode());
        assertFalse(bodyOf(response).contains(":NaN"), "response must not contain a raw NaN token");
    }

    @Test
    void rejectsInfinityEvenThoughJavaParsesItAsANumber() {
        HttpResponse response = SquareService.handle(Map.of("number", "Infinity"));
        assertEquals(400, response.getStatusCode());
        assertFalse(bodyOf(response).contains(":Infinity"), "response must not contain a raw Infinity token");
    }

    @Test
    void rejectsAFiniteInputWhoseSquareOverflowsToInfinity() {
        HttpResponse response = SquareService.handle(Map.of("number", "1e300"));
        assertEquals(400, response.getStatusCode());
        assertFalse(bodyOf(response).contains(":Infinity"), "response must not contain a raw Infinity token");
    }

    private static String bodyOf(HttpResponse response) {
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }
}
