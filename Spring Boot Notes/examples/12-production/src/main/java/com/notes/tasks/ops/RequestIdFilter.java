package com.notes.tasks.ops;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Gives every request an id, puts it in the MDC so EVERY log line of that request shows it,
 * and returns it in the X-Request-Id response header (a client can quote it in a bug report).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)              // before Spring Security, so even 401/403 lines get an id
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = req.getHeader(HEADER);                       // reuse the caller's id (e.g. from a gateway)
        if (id == null || !id.matches("[\\w-]{1,36}")) {         // ...but never log arbitrary client input
            id = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("requestId", id);
        res.setHeader(HEADER, id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("requestId");                             // threads are reused: always clean up
        }
    }
}
