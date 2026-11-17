package com.notes.tasks.events;

/** An application event is any object. A record keeps it immutable and small (ids, not entities).
 *  File 16 adds projectId (the Kafka key: per-project ordering) and priority (for the message). */
public record TaskCreatedEvent(long taskId, long projectId, String title, String priority, String createdBy) {}
