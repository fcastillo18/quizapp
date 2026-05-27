package com.fsl.quizapp.notification;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Configures asynchronous execution for notification dispatch. */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  /**
   * Creates a bounded thread pool dedicated to notification processing.
   *
   * @return executor named {@code notificationExecutor}
   */
  @Bean(name = "notificationExecutor")
  public Executor notificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("notification-");
    executor.initialize();
    return executor;
  }

  /**
   * Returns a handler that logs uncaught exceptions thrown from {@code @Async} methods without
   * swallowing them silently.
   *
   * @return the uncaught exception handler
   */
  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (Throwable ex, Method method, Object... params) ->
        log.error("Uncaught async exception in {}: {}", method.getName(), ex.getMessage(), ex);
  }
}
