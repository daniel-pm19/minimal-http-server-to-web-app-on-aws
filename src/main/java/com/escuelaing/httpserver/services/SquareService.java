package com.escuelaing.httpserver.services;

import com.escuelaing.httpserver.HttpResponse;
import com.escuelaing.httpserver.Json;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/** GET /square?number=... -&gt; {"input":x,"square":x*x} */
public final class SquareService {

    private SquareService() {
    }

    public static HttpResponse handle(Map<String, String> query) {
        String rawNumber = query.get("number");
        if (rawNumber == null || rawNumber.isBlank()) {
            return HttpResponse.badRequestJson("{\"error\":\"Missing required query parameter: number\"}");
        }

        double number;
        try {
            number = Double.parseDouble(rawNumber.trim());
        } catch (NumberFormatException e) {
            return HttpResponse.badRequestJson(
                    "{\"error\":\"'" + Json.escape(rawNumber) + "' is not a valid number\"}");
        }

        // Double.parseDouble accepts the literal text "NaN"/"Infinity"/"-Infinity"
        // as valid numbers, and squaring a large-enough finite value can itself
        // overflow to infinity. Either way the result would serialize as a bare
        // NaN/Infinity token, which is not valid JSON - reject it instead.
        if (!Double.isFinite(number)) {
            return HttpResponse.badRequestJson(
                    "{\"error\":\"'" + Json.escape(rawNumber) + "' is not a finite number\"}");
        }

        double square = number * number;
        if (!Double.isFinite(square)) {
            return HttpResponse.badRequestJson(
                    "{\"error\":\"The square of '" + Json.escape(rawNumber) + "' is too large to represent\"}");
        }
        String json = String.format(Locale.US, "{\"input\":%s,\"square\":%s}", format(number), format(square));
        return HttpResponse.ok("application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static String format(double value) {
        if (!Double.isInfinite(value) && value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
