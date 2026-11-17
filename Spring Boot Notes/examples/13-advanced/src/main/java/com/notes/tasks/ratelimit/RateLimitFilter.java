package com.notes.tasks.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiting per caller for /api/**. Each caller has a bucket of `capacity` tokens;
 * a request takes one; tokens come back at `refillPerSecond`. Empty bucket -> 429 Too Many Requests.
 * Registered without @Order, so it runs AFTER Spring Security's filter: the caller is already known.
 * In-memory: limits are per instance. Several instances need a shared store (Redis, Bucket4j).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int capacity;
    private final double refillPerSecond;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.capacity:20}") int capacity,
                           @Value("${app.rate-limit.refill-per-second:5}") double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String key = auth != null && auth.isAuthenticated() ? "user:" + auth.getName() : "ip:" + req.getRemoteAddr();
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillPerSecond));
        long waitSeconds = bucket.tryTake();
        res.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        if (waitSeconds == 0) {
            res.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.available()));
            chain.doFilter(req, res);
            return;
        }
        res.setStatus(429);
        res.setHeader("X-RateLimit-Remaining", "0");
        res.setHeader("Retry-After", String.valueOf(waitSeconds));        // seconds until a token is back
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");                                 // the servlet default is ISO-8859-1
        res.getWriter().write("{\"title\":\"Too Many Requests\",\"status\":429,"
                + "\"detail\":\"Rate limit of " + capacity + " requests exceeded; retry in " + waitSeconds + " s\"}");
    }

    /** Thread-safe bucket; tokens are refilled lazily from the elapsed time. */
    static final class TokenBucket {
        private final int capacity;
        private final double refillPerNano;
        private double tokens;
        private long last = System.nanoTime();

        TokenBucket(int capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerNano = refillPerSecond / 1_000_000_000.0;
            this.tokens = capacity;                                      // start full
        }

        /** 0 = allowed; otherwise the whole seconds to wait for the next token. */
        synchronized long tryTake() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - last) * refillPerNano);
            last = now;
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            return (long) Math.ceil((1 - tokens) / refillPerNano / 1_000_000_000.0);
        }

        synchronized int available() {
            return (int) tokens;
        }
    }
}
