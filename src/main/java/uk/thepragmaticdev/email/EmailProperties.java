package uk.thepragmaticdev.email;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mailgun")
public class EmailProperties {

  private String url;

  private String domain;

  private String secretKey;

  private From from;

  private List<Template> templates;

  @Data
  public static class From {

    private String name;

    private String email;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Template {

    private String name;

    private String subject;
  }
}