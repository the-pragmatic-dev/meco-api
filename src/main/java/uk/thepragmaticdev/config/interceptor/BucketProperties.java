package uk.thepragmaticdev.config.interceptor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bucket")
public class BucketProperties {

  private long capacity;

  private long tokens;

  private long minutes;
}