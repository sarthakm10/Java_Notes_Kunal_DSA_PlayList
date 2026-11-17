package com.notes.tasks.web;

import com.notes.tasks.config.TaskTrackerProperties;
import com.notes.tasks.notify.Notifier;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** A first taste of file 05: a record returned from a controller becomes JSON automatically. */
@RestController
public class InfoController {
    private final TaskTrackerProperties props;
    private final Notifier notifier;
    private final Environment env;

    public InfoController(TaskTrackerProperties props, Notifier notifier, Environment env) {
        this.props = props;
        this.notifier = notifier;
        this.env = env;
    }

    public record Info(String name, List<String> profiles, int maxOpenTasks,
                       String reminderEvery, String notifier) {}

    @GetMapping("/api/info")
    public Info info() {
        return new Info(props.displayName(), List.of(env.getActiveProfiles()),
                props.maxOpenTasks(), props.reminderEvery().toString(), notifier.describe());
    }
}
