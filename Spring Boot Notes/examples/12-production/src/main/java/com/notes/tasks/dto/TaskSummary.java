package com.notes.tasks.dto;

import com.notes.tasks.model.Priority;

import java.time.LocalDate;

/** Filled directly by a JPQL "select new ..." query: no entity is loaded. */
public record TaskSummary(long id, String title, Priority priority, LocalDate dueDate, String assignee) {}
