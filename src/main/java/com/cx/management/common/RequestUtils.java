package com.cx.management.common;

import javax.servlet.http.HttpServletRequest;

public final class RequestUtils {

    private RequestUtils() {}

    public static String getRequestMethod(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getMethod() + " " + request.getRequestURI();
    }
}
