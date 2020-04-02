package uk.thepragmaticdev.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailgunConfig {

  private final String domain;
  private final String secretKey;
  private final String fromName;
  private final String fromEmail;

  /**
   * TODO.
   * 
   * @param domain    TODO
   * @param secretKey TODO
   * @param fromName  TODO
   * @param fromEmail TODO
   */
  public MailgunConfig(//
      @Value("${mailgun.domain}") String domain, //
      @Value("${mailgun.secret-key}") String secretKey, //
      @Value("${mailgun.from.name}") String fromName, //
      @Value("${mailgun.from.email}") String fromEmail) {
    this.domain = domain;
    this.secretKey = secretKey;
    this.fromName = fromName;
    this.fromEmail = fromEmail;
  }

  /**
   * TODO.
   * 
   * @return
   */
  @Bean
  public net.sargue.mailgun.Configuration configuration() {
    return new net.sargue.mailgun.Configuration() //
        .domain(domain) //
        .apiKey(secretKey) //
        .from(fromName, fromEmail);
  }
}
