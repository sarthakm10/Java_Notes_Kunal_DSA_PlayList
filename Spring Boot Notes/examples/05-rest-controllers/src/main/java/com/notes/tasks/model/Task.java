package com.notes.tasks.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The internal model (it becomes a JPA entity in file 08, and entities must be mutable
 * classes, not records). Lombok writes the getters, setters, constructors and builder.
 * It is never sent to clients directly: controllers return DTOs instead.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Task {
    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private boolean done;
    private LocalDate dueDate;
    private Instant createdAt;
    private String internalNotes;          // must never leak to the API
}
