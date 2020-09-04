package uk.thepragmaticdev.text;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "perspective")
public class TextProperties {

  private String url;

  private String secretKey;
}
