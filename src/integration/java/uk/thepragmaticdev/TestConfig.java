package uk.thepragmaticdev;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.thepragmaticdev.email.EmailService;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public EmailService mockEmailService() {
    return mock(EmailService.class);
  }
}