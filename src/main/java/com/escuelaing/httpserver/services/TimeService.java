package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** GET /time -&gt; {"serverTime":"<ISO-8601 instant>"} - proves the value comes from the server, not the browser. */
public final class TimeService {

    private TimeService() {
    }

    public static HttpResponse handle() {
        String json = "{\"serverTime\":\"" + Instant.now() + "\"}";
        return HttpResponse.ok("application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }
}
