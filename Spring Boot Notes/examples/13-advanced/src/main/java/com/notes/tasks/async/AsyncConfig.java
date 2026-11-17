package com.notes.tasks.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync                                   // @Async methods run on an executor, through a proxy
@EnableScheduling                              // @Scheduled methods run on the task scheduler
public class AsyncConfig {

    /**
     * A bounded pool for slow report jobs, separate from request handling.
     * How a ThreadPoolExecutor grows: core threads first, then the QUEUE fills, then up to max threads,
     * then the rejection policy. Here: 2 core + 2 queued + 2 extra = 6 jobs at once; the 7th is rejected.
     */
    @Bean
    ThreadPoolTaskExecutor reportExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(2);
        ex.setThreadNamePrefix("report-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());   // throw when full
        ex.setWaitForTasksToCompleteOnShutdown(true);       // graceful: finish running jobs on shutdown...
        ex.setAwaitTerminationSeconds(10);                  // ...but wait at most 10 s
        return ex;                                          // Spring calls initialize() for us
    }
}
