package uk.thepragmaticdev.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "async")
public class AsyncProperties {

  private int corePoolSize;

  private int maxPoolSize;

  private int queueCapacity;
}