package com.cx.management.common;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IpUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private static final Pattern FORWARDED_FOR_PATTERN =
            Pattern.compile("for=\"?\\[?([^;,\"\\]]+)\\]?\"?", Pattern.CASE_INSENSITIVE);

    private IpUtils() {}

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwarded = parseForwardedHeader(request.getHeader("Forwarded"));
        if (isValidIp(forwarded)) {
            return normalizeIp(forwarded);
        }

        for (String header : IP_HEADERS) {
            String ip = pickIpFromChain(request.getHeader(header));
            if (isValidIp(ip)) {
                return normalizeIp(ip);
            }
        }

        return normalizeIp(request.getRemoteAddr());
    }

    private static String parseForwardedHeader(String forwarded) {
        if (!StringUtils.hasText(forwarded)) {
            return null;
        }
        Matcher matcher = FORWARDED_FOR_PATTERN.matcher(forwarded);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String pickIpFromChain(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }
        String[] parts = headerValue.split(",");
        String fallback = null;
        for (String part : parts) {
            String ip = part.trim();
            if (!isValidIp(ip)) {
                continue;
            }
            fallback = ip;
            if (!isLoopback(ip) && !isPrivateIp(ip)) {
                return ip;
            }
        }
        return fallback;
    }

    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    private static boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }

    private static boolean isPrivateIp(String ip) {
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] seg = ip.split("\\.");
            if (seg.length > 1) {
                try {
                    int second = Integer.parseInt(seg[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return ip;
        }
        ip = ip.trim();
        if (ip.startsWith("::ffff:")) {
            ip = ip.substring(7);
        }
        if (isLoopback(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
