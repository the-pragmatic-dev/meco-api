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
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Service
public class EmailService {

  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  private final WebClient webClient;
  private final String domain;
  private final String fromName;
  private final String fromEmail; // TODO: might be used

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
    formData.add("v:username", account.getUsername());
    formData.add("v:token", account.getPasswordResetToken());
    send(account.getUsername(), "Reset your MECO password", "forgotten-password", formData);
  }

  /**
   * Send an email informing the user their password has been updated.
   * 
   * @param account The account that was updated
   */
  public void sendResetPassword(Account account) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:username", account.getUsername());
    send(account.getUsername(), "Your MECO password has been changed", "reset-password", formData);
  }

  /**
   * Send an email informing the user that an unrecognized device signed in to the
   * account.
   * 
   * @param account         The account that was signed in
   * @param requestMetadata The geolocation and device information of the request
   */
  public void sendUnrecognizedDevice(Account account, RequestMetadata requestMetadata) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:time", requestMetadata.getCreatedDate().toString());
    formData.add("v:cityName", requestMetadata.getGeoMetadata().getCityName());
    formData.add("v:countryIsoCode", requestMetadata.getGeoMetadata().getCountryIsoCode());
    formData.add("v:subdivisionIsoCode", requestMetadata.getGeoMetadata().getSubdivisionIsoCode());
    formData.add("v:operatingSystemFamily", requestMetadata.getDeviceMetadata().getOperatingSystemFamily());
    formData.add("v:userAgentFamily", requestMetadata.getDeviceMetadata().getUserAgentFamily());
    send(account.getUsername(), "Unrecognized device signed in to your MECO account", "unrecognized-device", formData);
  }

  /**
   * Send an email informing the user a new key has been created.
   * 
   * @param account The account the key was created on
   */
  public void sendKeyCreated(Account account, ApiKey createdKey) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:name", createdKey.getName());
    formData.add("v:prefix", createdKey.getPrefix());
    send(account.getUsername(), "A new key was added to your account", "new-key", formData);
  }

  /**
   * Send an email informing the user a key has been deleted.
   * 
   * @param account The account the key was deleted on
   */
  public void sendKeyDeleted(Account account, ApiKey deletedKey) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:name", deletedKey.getName());
    formData.add("v:prefix", deletedKey.getPrefix());
    send(account.getUsername(), "A key was deleted from your account", "delete-key", formData);
  }

  private void send(String to, String subject, String template, MultiValueMap<String, String> formData) {
    logger.info("webclient:send: to={}, subject={}, template={}, formData={}", to, subject, template,
        formData.toString());
    webClient.post()//
        .uri(uriBuilder -> uriBuilder.path("/messages")//
            .queryParam("from", String.format("%s <mailgun@%s.mailgun.org>", fromName, domain))//
            .queryParam("to", to)//
            .queryParam("subject", subject)//
            .queryParam("template", template).build())
        .body(BodyInserters.fromFormData(formData)).retrieve()//
        .onStatus(HttpStatus::is4xxClientError, error -> {
          logger.error("webclient:error: status={}, body={}", error.statusCode(), error.bodyToMono(String.class));
          return Mono.empty();
        })//
        .onStatus(HttpStatus::is5xxServerError, error -> {
          logger.error("webclient:error: status={}, body={}", error.statusCode(), error.bodyToMono(String.class));
          return Mono.empty();
        })//
        .toBodilessEntity().subscribe();
  }
}