package uk.thepragmaticdev;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import uk.thepragmaticdev.email.EmailService;

@TestConfiguration
public class IntegrationConfig {

  /**
   * Creates a primary rest template builder bean to be used for integration
   * tests.
   * 
   * @return a mock rest template builder
   */
  @Bean
  @Primary
  @Profile("http-disabled")
  public RestTemplateBuilder mockRestTemplateBuilder() {
    var builder = mock(RestTemplateBuilder.class);
    var restTemplate = mock(RestTemplate.class);
    when(builder.errorHandler(any())).thenReturn(builder);
    when(builder.basicAuthentication(any(), any())).thenReturn(builder);
    when(builder.build()).thenReturn(restTemplate);
    return builder;
  }

  @Bean
  @Primary
  @Profile("async-disabled")
  public EmailService mockEmailService() {
    return mock(EmailService.class);
  }
}