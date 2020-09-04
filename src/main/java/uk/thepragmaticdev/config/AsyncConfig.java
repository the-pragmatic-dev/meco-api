package uk.thepragmaticdev.config;

import java.util.concurrent.Executor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.thepragmaticdev.exception.handler.AsyncErrorHandler;

@Configuration
@EnableAsync
@Profile("!async-disabled")
public class AsyncConfig implements AsyncConfigurer {

  private final AsyncProperties properties;

  public AsyncConfig(AsyncProperties properties) {
    this.properties = properties;
  }

  @Override
  public Executor getAsyncExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getCorePoolSize());
    executor.setMaxPoolSize(properties.getMaxPoolSize());
    executor.setQueueCapacity(properties.getQueueCapacity());
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncErrorHandler();
  }
}