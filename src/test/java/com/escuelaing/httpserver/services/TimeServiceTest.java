package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TimeServiceTest {

    @Test
    void returnsAParsableServerTimestampWithinTheCallWindow() {
        Instant before = Instant.now();
        HttpResponse response = TimeService.handle();
        Instant after = Instant.now();

        assertEquals(200, response.getStatusCode());
        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        String timestamp = body.replaceAll(".*\"serverTime\":\"(.*)\"}", "$1");
        Instant reported = Instant.parse(timestamp);

        assertFalse(reported.isBefore(before));
        assertFalse(reported.isAfter(after));
    }
}
