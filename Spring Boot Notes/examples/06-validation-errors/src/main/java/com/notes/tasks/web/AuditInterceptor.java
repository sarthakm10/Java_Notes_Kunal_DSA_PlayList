package com.notes.tasks.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A Spring MVC INTERCEPTOR: runs inside DispatcherServlet, AFTER the controller method has
 * been chosen, so it knows WHICH method (and its annotations) will handle the request.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {
    private final RequestInfo requestInfo;

    public AuditInterceptor(RequestInfo requestInfo) {
        this.requestInfo = requestInfo;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (handler instanceof HandlerMethod hm) {
            String method = hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName();
            AuditLog audit = hm.getMethodAnnotation(AuditLog.class);
            System.out.println("[interceptor] preHandle  request #" + requestInfo.number() + " -> " + method
                    + (audit != null ? "  [audit: " + audit.value() + "]" : ""));
        }
        if ("true".equals(req.getHeader("X-Block"))) {       // returning false stops the request here
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            System.out.println("[interceptor] blocked by X-Block header");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        System.out.println("[interceptor] afterCompletion request #" + requestInfo.number()
                + " status " + res.getStatus());
    }
}
