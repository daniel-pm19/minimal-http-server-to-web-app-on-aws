package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthServiceTest {

    @Test
    void reportsUp() {
        HttpResponse response = HealthService.handle();
        assertEquals(200, response.getStatusCode());
        assertEquals("{\"status\":\"UP\"}", new String(response.getBody(), StandardCharsets.UTF_8));
    }
}
