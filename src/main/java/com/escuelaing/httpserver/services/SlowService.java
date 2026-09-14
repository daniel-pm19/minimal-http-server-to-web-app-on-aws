package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;

import java.nio.charset.StandardCharsets;

/**
 * GET /slow - not one of the four required services. It exists only to
 * demonstrate, locally or on EC2, that this server is sequential: while it
 * sleeps, every other connection queues behind it. See report section 6.2.
 */
public final class SlowService {

    private static final int DELAY_MILLIS = 5000;

    private SlowService() {
    }

    public static HttpResponse handle() {
        try {
            Thread.sleep(DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String json = "{\"message\":\"This response was delayed on purpose\",\"delayMs\":" + DELAY_MILLIS + "}";
        return HttpResponse.ok("application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }
}
