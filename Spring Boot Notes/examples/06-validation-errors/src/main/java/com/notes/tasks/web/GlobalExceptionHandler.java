package com.notes.tasks.web;

import com.notes.tasks.service.TaskNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One place that turns exceptions into RFC 9457 "Problem Details" JSON for every controller.
 * Extending ResponseEntityExceptionHandler gives ProblemDetail responses for all of Spring MVC's
 * own exceptions (400 bad JSON, 405, 415, 404 no route, 413...) for free.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Our domain exception -> 404
    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleNotFound(TaskNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://notes.example/problems/task-not-found"));
        pd.setTitle("Task not found");
        pd.setProperty("taskId", ex.getTaskId());
        return pd;
    }

    // 2. @Valid @RequestBody failed -> 400 with one entry per invalid field
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ex.getBody();                         // Spring already built a 400 ProblemDetail
        pd.setTitle("Validation failed");
        pd.setDetail(ex.getErrorCount() + " field(s) are invalid");
        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .sorted((a, b) -> a.getField().compareTo(b.getField()))
                .map(GlobalExceptionHandler::toMap)
                .toList();
        pd.setProperty("errors", errors);
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    // 3. Constraint on a @PathVariable / @RequestParam failed (built-in method validation) -> 400
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ex.getBody();
        pd.setTitle("Validation failed");
        List<String> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .toList();
        pd.setProperty("errors", errors);
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    // 4. Anything unexpected -> 500, WITHOUT leaking the internal message to the client
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. It has been logged.");
        pd.setTitle("Internal error");
        return pd;
    }

    private static Map<String, Object> toMap(FieldError fe) {
        Map<String, Object> m = new LinkedHashMap<>();         // keeps this key order in the JSON
        m.put("field", fe.getField());
        m.put("message", fe.getDefaultMessage());
        m.put("rejected", fe.getRejectedValue());
        return m;
    }
}
