package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;

import java.nio.charset.StandardCharsets;

/** GET /health -&gt; {"status":"UP"} - confirms the process can serve requests. */
public final class HealthService {

    private HealthService() {
    }

    public static HttpResponse handle() {
        String json = "{\"status\":\"UP\"}";
        return HttpResponse.ok("application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }
}
