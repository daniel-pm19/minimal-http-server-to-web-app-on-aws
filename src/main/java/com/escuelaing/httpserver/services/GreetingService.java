package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;
import com.escuelaing.httpserver.Json;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** GET /greeting?name=... -&gt; {"message":"Hello, <name>!"} */
public final class GreetingService {

    private GreetingService() {
    }

    public static HttpResponse handle(Map<String, String> query) {
        String name = query.get("name");
        if (name == null || name.isBlank()) {
            return HttpResponse.badRequestJson("{\"error\":\"Missing required query parameter: name\"}");
        }

        String json = "{\"message\":\"Hello, " + Json.escape(name.trim()) + "!\"}";
        return HttpResponse.ok("application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }
}
