package com.notes.tasks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning side of Project.tasks: this table has the project_id FK. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Unidirectional many-to-one: AppUser has no list of tasks. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)              // store "HIGH", not the ordinal 2
    @Column(nullable = false, length = 6)
    private Priority priority = Priority.MEDIUM;

    private boolean done;

    private LocalDate dueDate;                // column name: due_date (camelCase -> snake_case)

    @Embedded
    private AuditInfo audit;

    /** Optimistic locking: Hibernate adds "and version=?" to every UPDATE and increments it. */
    @Version
    private long version;

    @ManyToMany
    @JoinTable(name = "task_tag",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    public Task(String title, Priority priority, AuditInfo audit) {
        this.title = title;
        this.priority = priority;
        this.audit = audit;
    }
}
