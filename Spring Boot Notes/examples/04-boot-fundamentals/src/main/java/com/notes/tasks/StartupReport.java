package com.notes.tasks;

import com.notes.tasks.config.TaskTrackerProperties;
import com.notes.tasks.notify.Notifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** Runs once, after the context has started (and after Tomcat is listening). */
@Component
public class StartupReport implements ApplicationRunner {
    private final TaskTrackerProperties props;
    private final Notifier notifier;
    private final Environment env;
    private final ApplicationContext ctx;

    public StartupReport(TaskTrackerProperties props, Notifier notifier, Environment env, ApplicationContext ctx) {
        this.props = props;
        this.notifier = notifier;
        this.env = env;
        this.ctx = ctx;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("=== Startup report ===");
        System.out.println("active profiles : " + Arrays.toString(env.getActiveProfiles()));
        System.out.println("app name        : " + env.getProperty("spring.application.name"));
        System.out.println("properties      : " + props);
        System.out.println("notifier        : " + notifier.describe());
        System.out.println("server port     : " + env.getProperty("local.server.port"));
        System.out.println("bean count      : " + ctx.getBeanDefinitionCount());
        System.out.println("auto-configured : jsonMapper=" + ctx.containsBean("jacksonJsonMapper")
                + ", dispatcherServlet=" + ctx.containsBean("dispatcherServlet")
                + ", dataSource=" + ctx.containsBean("dataSource"));
        System.out.println("non-option args : " + args.getNonOptionArgs());
        System.out.println("======================");
    }
}
