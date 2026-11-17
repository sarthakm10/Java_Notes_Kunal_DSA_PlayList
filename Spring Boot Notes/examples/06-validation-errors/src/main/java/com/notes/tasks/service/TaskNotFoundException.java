package com.notes.tasks.service;

/** A plain domain exception: no HTTP knowledge. The web layer's GlobalExceptionHandler maps it to 404. */
public class TaskNotFoundException extends RuntimeException {
    private final long taskId;

    public TaskNotFoundException(long taskId) {
        super("Task " + taskId + " does not exist");
        this.taskId = taskId;
    }

    public long getTaskId() { return taskId; }
}
