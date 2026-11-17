package com.notes.core;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service                       // a singleton...
public class ReportService {
    private final TaskRepository repo;
    private final ObjectProvider<ReportBuilder> builders;   // ...that needs a fresh prototype each call

    public ReportService(TaskRepository repo, ObjectProvider<ReportBuilder> builders) {
        this.repo = repo;
        this.builders = builders;
    }

    public String report() {
        ReportBuilder b = builders.getObject();             // new ReportBuilder every time
        repo.findAll().forEach(t -> b.line("#" + t.id() + " " + t.title() + " (" + t.owner() + ")"));
        return b.build();
    }
}
