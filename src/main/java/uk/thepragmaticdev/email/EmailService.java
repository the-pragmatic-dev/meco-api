package uk.thepragmaticdev.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.thepragmaticdev.account.Account;

@Service
public class EmailService {

  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  private final WebClient webClient;
  private final String domain;
  private final String fromName;
  private final String fromEmail;

  /**
   * Service for sending emails. Provides default mailgun configuration.
   * 
   * @param domain    The Mailgun domain
   * @param secretKey The Mailgun api key
   * @param fromName  The default sender name
   * @param fromEmail The default sender email address
   */
  public EmailService(//
      @Value("${mailgun.domain}") String domain, //
      @Value("${mailgun.secret-key}") String secretKey, //
      @Value("${mailgun.from.name}") String fromName, //
      @Value("${mailgun.from.email}") String fromEmail, //
      WebClient.Builder webClientBuilder) {
    this.domain = domain;
    this.fromName = fromName;
    this.fromEmail = fromEmail;
    this.webClient = webClientBuilder.baseUrl(String.format("https://api.mailgun.net/v3/%s.mailgun.org", domain))//
        .defaultHeaders(header -> header.setBasicAuth("api", secretKey))//
        .build();
  }

  /**
   * Send a welcome email when a new account is created.
   * 
   * @param account The newly created account
   */
  public void sendAccountCreated(Account account) {
    send(account.getUsername(), "Welcome to MECO!", "new-account", new LinkedMultiValueMap<>());
  }

  /**
   * Send a forgotten password email to user with reset token.
   * 
   * @param account The account requesting a password reset
   */
  public void sendForgottenPassword(Account account) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:token", account.getPasswordResetToken());
    send(account.getUsername(), "Reset your MECO password", "forgotten-password", formData);
  }

  private void send(String to, String subject, String template, MultiValueMap<String, String> formData) {
    webClient.post()//
        .uri(uriBuilder -> uriBuilder.path("/messages")//
            .queryParam("from", String.format("%s <mailgun@%s.mailgun.org>", fromName, domain))//
            .queryParam("to", to)//
            .queryParam("subject", subject)//
            .queryParam("template", template).build())
        .body(BodyInserters.fromFormData(formData)).retrieve()//
        .onStatus(HttpStatus::is4xxClientError, error -> {
          logger.error("webclient - status {}, body {}", error.statusCode(), error.bodyToMono(String.class));
          return Mono.empty();
        })//
        .onStatus(HttpStatus::is5xxServerError, error -> {
          logger.error("webclient - status {}, body {}", error.statusCode(), error.bodyToMono(String.class));
          return Mono.empty();
        })//
        .toBodilessEntity().subscribe();
  }
}