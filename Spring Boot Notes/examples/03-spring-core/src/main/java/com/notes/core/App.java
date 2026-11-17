package com.notes.core;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Clock;
import java.util.Arrays;

public class App {
    public static void main(String[] args) {
        String profile = args.length > 0 ? args[0] : "dev";
        System.out.println("1) Starting the container with profile '" + profile + "'");

        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().setActiveProfiles(profile);
            ctx.register(AppConfig.class);
            ctx.refresh();                       // scan, create and wire all singleton beans NOW

            System.out.println("\n2) Our beans in the container:");
            Arrays.stream(ctx.getBeanDefinitionNames())
                  .filter(n -> !n.startsWith("org.springframework"))
                  .sorted()
                  .forEach(n -> System.out.println("   - " + n + " : " + ctx.getType(n).getSimpleName()));

            System.out.println("\n3) Using the services:");
            TaskService tasks = ctx.getBean(TaskService.class);
            System.out.println("  " + tasks.addTask("Learn DI", "asha"));
            System.out.println("  " + tasks.addTask("Learn bean scopes", "ravi"));
            System.out.println("  " + tasks.addTask("Learn AOP", "ravi"));

            NotificationService ns = ctx.getBean(NotificationService.class);
            System.out.println("  channels injected as a List: " + ns.channels());

            System.out.println("\n4) Singleton vs prototype:");
            System.out.println("  same TaskService twice?   " + (tasks == ctx.getBean(TaskService.class)));
            System.out.println("  same ReportBuilder twice? "
                    + (ctx.getBean(ReportBuilder.class) == ctx.getBean(ReportBuilder.class)));
            ReportService reports = ctx.getBean(ReportService.class);
            System.out.print(reports.report());
            System.out.print(reports.report());

            System.out.println("\n5) Profiles:");
            System.out.println("  Clock bean: " + ctx.getBean(Clock.class));
            try {
                ctx.getBean("systemClock");
            } catch (NoSuchBeanDefinitionException e) {
                System.out.println("  no 'systemClock' bean: the 'prod' profile is not active");
            }

            System.out.println("\n6) Closing the container:");
        }                                        // try-with-resources calls ctx.close()
        System.out.println("Done.");
    }
}
