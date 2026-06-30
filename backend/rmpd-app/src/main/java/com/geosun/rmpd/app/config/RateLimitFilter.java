package com.geosun.rmpd.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequestsPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${rmpd.rate-limit.per-minute:10}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isRateLimited(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = resolveKey(request);
        Window window = windows.computeIfAbsent(key, k -> new Window());
        if (window.tryConsume(maxRequestsPerMinute)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Забагато запитів, спробуйте пізніше\"}");
        }
    }

    private boolean isRateLimited(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/submit") || path.contains("/cmr/upload");
    }

    private String resolveKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return auth != null ? auth : request.getRemoteAddr();
    }

    private static final class Window {
        private volatile long minuteEpoch = Instant.now().getEpochSecond() / 60;
        private final AtomicInteger count = new AtomicInteger();

        boolean tryConsume(int limit) {
            long currentMinute = Instant.now().getEpochSecond() / 60;
            if (currentMinute != minuteEpoch) {
                synchronized (this) {
                    if (currentMinute != minuteEpoch) {
                        minuteEpoch = currentMinute;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
