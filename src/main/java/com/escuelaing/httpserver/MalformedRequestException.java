package com.escuelaing.httpserver;

/**
 * Thrown when the bytes coming from a client socket cannot be parsed as an
 * HTTP request line. The server catches this per-connection so one bad
 * request never brings down the accept loop.
 */
public class MalformedRequestException extends RuntimeException {

    public MalformedRequestException(String message) {
        super(message);
    }
}
