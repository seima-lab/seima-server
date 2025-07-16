package vn.fpt.seima.seimaserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing
 * Enables async notifications without blocking main thread
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Task executor for notification processing
     * Separate thread pool for notifications to avoid blocking main operations
     */
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - always kept alive
        executor.setCorePoolSize(2);
        
        // Maximum pool size - can grow up to this
        executor.setMaxPoolSize(10);
        
        // Queue capacity - pending tasks queue
        executor.setQueueCapacity(100);
        
        // Thread name prefix
        executor.setThreadNamePrefix("NotificationAsync-");
        
        // Keep alive time for extra threads
        executor.setKeepAliveSeconds(60);
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * General task executor for other async operations
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("AsyncTask-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
} 