package com.notes.tasks.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A servlet FILTER: runs in Tomcat, BEFORE Spring's DispatcherServlet, for every request
 * (even ones no controller handles). It sees raw requests/responses, not controller methods.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String what = request.getMethod() + " " + request.getRequestURI();
        System.out.println("[filter]      before " + what);
        response.setHeader("X-Filtered", "yes");
        chain.doFilter(request, response);                   // continue to the next filter / servlet
        System.out.println("[filter]      after  " + what + " -> " + response.getStatus());
    }
}
