package com.notes.ms.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * One line per request at the edge. It runs inside Boot's server observation, so the line carries the
 * trace id that the gateway starts and then sends on to the services (W3C traceparent header).
 */
@Component
public class AccessLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long t0 = System.nanoTime();
        try {
            chain.doFilter(req, res);
            log.info("{} {} -> {} ({} ms)", req.getMethod(), req.getRequestURI(), res.getStatus(), ms(t0));
        } catch (ServletException | IOException | RuntimeException e) {
            // Not res.getStatus() in a finally block: it still says 200 here, because Tomcat turns the
            // exception into a 500 only AFTER it has left this filter (the first version logged "-> 200")
            log.warn("{} {} -> failed after {} ms: {}", req.getMethod(), req.getRequestURI(), ms(t0), rootCause(e));
            throw e;
        }
    }

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static String rootCause(Throwable e) {
        while (e.getCause() != null) e = e.getCause();
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
