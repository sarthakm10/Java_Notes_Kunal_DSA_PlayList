package com.notes.tasks.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

/**
 * An exception that carries its own ProblemDetail: Spring MVC renders ErrorResponseException
 * subclasses as RFC 9457 responses without any handler method.
 */
public class TooManyOpenTasksException extends ErrorResponseException {

    public TooManyOpenTasksException(int max) {
        super(HttpStatus.CONFLICT, problem(max), null);
    }

    private static ProblemDetail problem(int max) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "You already have " + max + " open tasks. Finish one first.");
        pd.setType(URI.create("https://notes.example/problems/too-many-open-tasks"));
        pd.setTitle("Too many open tasks");
        pd.setProperty("maxOpenTasks", max);
        return pd;
    }
}
