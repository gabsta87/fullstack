package com.serv.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Work done outside HTTP requests, each kind on its own bounded executor. */
@Configuration
public class BackgroundTasksConfig {

    /** One video is re-encoded at a time (CPU heavy); the others wait in a bounded queue. */
    @Bean
    public ThreadPoolTaskExecutor videoProcessingExecutor() {
        return executor("video-", 1, 200);
    }

    /** Emails sent later than the request that triggers them (back-online alerts). */
    @Bean
    public ThreadPoolTaskExecutor mailExecutor() {
        return executor("mail-", 2, 1000);
    }

    private static ThreadPoolTaskExecutor executor(String prefix, int threads, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix(prefix);
        // Unfinished videos are resumed at the next start
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
