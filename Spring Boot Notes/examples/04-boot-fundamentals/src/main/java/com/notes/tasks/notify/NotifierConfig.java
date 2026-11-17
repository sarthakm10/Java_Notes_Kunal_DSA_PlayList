package com.notes.tasks.notify;

import com.notes.tasks.config.TaskTrackerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The same trick Spring Boot's own auto-configuration uses:
 * a bean that only exists when a property says so, and a fallback that only exists
 * when nobody else defined one.
 */
@Configuration(proxyBeanMethods = false)
public class NotifierConfig {

    @Bean
    @ConditionalOnProperty(name = "tasks.notifications.enabled", havingValue = "true")
    Notifier realNotifier(TaskTrackerProperties props) {
        String channel = props.notifications().channel();
        return () -> "sending real notifications by " + channel;
    }

    @Bean
    @ConditionalOnMissingBean(Notifier.class)    // only if realNotifier was NOT created
    Notifier noOpNotifier() {
        return () -> "notifications disabled (no-op notifier)";
    }
}
