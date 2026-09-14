package com.escuelaing.httpserver;

import java.util.Map;

/** Hardcoded extension -&gt; MIME type table, per the lab's fixed resource types. */
public final class ContentTypes {

    public static final String DEFAULT = "application/octet-stream";

    private static final Map<String, String> EXTENSION_TO_TYPE = Map.of(
            "html", "text/html; charset=UTF-8",
            "htm", "text/html; charset=UTF-8",
            "js", "text/javascript; charset=UTF-8",
            "css", "text/css; charset=UTF-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "ico", "image/x-icon"
    );

    private ContentTypes() {
    }

    public static String resolve(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return DEFAULT;
        }
        String extension = path.substring(dot + 1).toLowerCase();
        return EXTENSION_TO_TYPE.getOrDefault(extension, DEFAULT);
    }
}
