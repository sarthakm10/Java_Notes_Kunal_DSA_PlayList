package com.notes.tasks.events;

/** An application event is any object. A record keeps it immutable and small (ids, not entities). */
public record TaskCreatedEvent(long taskId, String title, String createdBy) {}
