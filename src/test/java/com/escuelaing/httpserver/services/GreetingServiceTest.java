package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreetingServiceTest {

    @Test
    void returnsGreetingForValidName() {
        HttpResponse response = GreetingService.handle(Map.of("name", "Daniel"));
        assertEquals(200, response.getStatusCode());
        assertEquals("{\"message\":\"Hello, Daniel!\"}", bodyOf(response));
    }

    @Test
    void escapesQuotesInName() {
        HttpResponse response = GreetingService.handle(Map.of("name", "\"Dan\""));
        assertEquals(200, response.getStatusCode());
        assertTrue(bodyOf(response).contains("\\\"Dan\\\""));
    }

    @Test
    void rejectsMissingName() {
        HttpResponse response = GreetingService.handle(Map.of());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void rejectsBlankName() {
        HttpResponse response = GreetingService.handle(Map.of("name", "   "));
        assertEquals(400, response.getStatusCode());
    }

    private static String bodyOf(HttpResponse response) {
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }
}
