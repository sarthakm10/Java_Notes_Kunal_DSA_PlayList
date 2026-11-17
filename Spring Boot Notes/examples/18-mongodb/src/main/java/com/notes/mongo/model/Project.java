package com.notes.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

/**
 * Projects are NOT embedded in tasks (tasks are queried on their own and a project has unbounded tasks),
 * and the owner is a REFERENCE: stored as the user's id, loaded with a second query when accessed.
 */
@Document("projects")
public class Project {
    @Id
    private String id;

    @Indexed(unique = true)
    private String key;                      // "TT", "WEB": what tasks point to

    private String name;

    @DocumentReference(lazy = true)          // stored as the User's id; fetched on first access
    private User owner;

    protected Project() {}

    public Project(String key, String name, User owner) {
        this.key = key;
        this.name = name;
        this.owner = owner;
    }

    public String getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public User getOwner() { return owner; }
}
