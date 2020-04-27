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
  private final String accountCreatedName;
  private final String accountCreatedSubject;
  private final String accountForgottenPasswordName;
  private final String accountForgottenPasswordSubject;
  private final String accountResetPasswordName;
  private final String accountResetPasswordSubject;
  private final String accountUnrecognizedDeviceName;
  private final String accountUnrecognizedDeviceSubject;
  private final String keyCreatedName;
  private final String keyCreatedSubject;
  private final String keyDeletedName;
  private final String keyDeletedSubject;

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
      @Value("${mailgun.template.account-created.name}") String accountCreatedName, //
      @Value("${mailgun.template.account-created.subject}") String accountCreatedSubject, //
      @Value("${mailgun.template.account-forgotten-password.name}") String accountForgottenPasswordName, //
      @Value("${mailgun.template.account-forgotten-password.subject}") String accountForgottenPasswordSubject, //
      @Value("${mailgun.template.account-reset-password.name}") String accountResetPasswordName, //
      @Value("${mailgun.template.account-reset-password.subject}") String accountResetPasswordSubject, //
      @Value("${mailgun.template.account-unrecognized-device.name}") String accountUnrecognizedDeviceName, //
      @Value("${mailgun.template.account-unrecognized-device.subject}") String accountUnrecognizedDeviceSubject, //
      @Value("${mailgun.template.key-created.name}") String keyCreatedName, //
      @Value("${mailgun.template.key-created.subject}") String keyCreatedSubject, //
      @Value("${mailgun.template.key-deleted.name}") String keyDeletedName, //
      @Value("${mailgun.template.key-deleted.subject}") String keyDeletedSubject, //
      WebClient.Builder webClientBuilder) {
    this.domain = domain;
    this.fromName = fromName;
    this.fromEmail = fromEmail;
    this.accountCreatedName = accountCreatedName;
    this.accountCreatedSubject = accountCreatedSubject;
    this.accountForgottenPasswordName = accountForgottenPasswordName;
    this.accountForgottenPasswordSubject = accountForgottenPasswordSubject;
    this.accountResetPasswordName = accountResetPasswordName;
    this.accountResetPasswordSubject = accountResetPasswordSubject;
    this.accountUnrecognizedDeviceName = accountUnrecognizedDeviceName;
    this.accountUnrecognizedDeviceSubject = accountUnrecognizedDeviceSubject;
    this.keyCreatedName = keyCreatedName;
    this.keyCreatedSubject = keyCreatedSubject;
    this.keyDeletedName = keyDeletedName;
    this.keyDeletedSubject = keyDeletedSubject;
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
    send(account.getUsername(), accountCreatedName, accountCreatedSubject, new LinkedMultiValueMap<>());
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
    send(account.getUsername(), accountForgottenPasswordName, accountForgottenPasswordSubject, formData);
  }

  /**
   * Send an email informing the user their password has been updated.
   * 
   * @param account The account that was updated
   */
  public void sendResetPassword(Account account) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:username", account.getUsername());
    send(account.getUsername(), accountResetPasswordName, accountResetPasswordSubject, formData);
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
    send(account.getUsername(), accountUnrecognizedDeviceName, accountUnrecognizedDeviceSubject, formData);
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
    send(account.getUsername(), keyCreatedName, keyCreatedSubject, formData);
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
    send(account.getUsername(), keyDeletedName, keyDeletedSubject, formData);
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