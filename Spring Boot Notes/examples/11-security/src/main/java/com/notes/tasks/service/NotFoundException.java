package com.notes.tasks.service;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String what, long id) {
        super(what + " " + id + " does not exist");
    }
}
