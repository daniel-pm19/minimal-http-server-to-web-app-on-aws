package com.escuelaing.httpserver;

import com.escuelaing.httpserver.services.GreetingService;
import com.escuelaing.httpserver.services.HealthService;
import com.escuelaing.httpserver.services.SlowService;
import com.escuelaing.httpserver.services.SquareService;
import com.escuelaing.httpserver.services.TimeService;

/**
 * Deliberately explicit route dispatch - one if/else chain, no reflection,
 * no annotations, no general-purpose routing framework. That keeps the
 * request-to-behavior mapping visible instead of hidden behind a library.
 */
public final class Router {

    private Router() {
    }

    public static HttpResponse route(HttpRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return HttpResponse.methodNotAllowed("GET");
        }

        String path = request.getPath();

        if (path.equals("/greeting")) {
            return GreetingService.handle(request.getQueryParams());
        }
        if (path.equals("/square")) {
            return SquareService.handle(request.getQueryParams());
        }
        if (path.equals("/time")) {
            return TimeService.handle();
        }
        if (path.equals("/health")) {
            return HealthService.handle();
        }
        if (path.equals("/slow")) {
            return SlowService.handle();
        }

        return StaticResourceHandler.serve(path);
    }
}
