package uk.thepragmaticdev.email;

import java.net.URI;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.email.EmailProperties.Template;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.exception.handler.RestTemplateErrorHandler;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.log.security.SecurityLog;

@Log4j2
@Service
public class EmailService {

  private final EmailProperties properties;

  private final RestTemplate restTemplate;

  /**
   * Service for sending emails. Provides default mailgun configuration.
   * 
   * @param properties The email properties
   * @param builder    The builder to create rest request
   */
  public EmailService(EmailProperties properties, RestTemplateBuilder builder) {
    this.properties = properties;
    this.restTemplate = builder.errorHandler(new RestTemplateErrorHandler())
        .basicAuthentication("api", properties.getSecretKey()).build();
  }

  /**
   * Send a welcome email when a new account is created.
   * 
   * @param account The newly created account
   */
  @Async
  public void sendAccountCreated(Account account) {
    send(account.getUsername(), "account-created", new LinkedMultiValueMap<>());
  }

  /**
   * Send a forgotten password email to user with reset token.
   * 
   * @param account The account requesting a password reset
   */
  @Async
  public void sendForgottenPassword(Account account) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:username", account.getUsername());
    formData.add("v:token", account.getPasswordResetToken());
    send(account.getUsername(), "account-forgotten-password", formData);
  }

  /**
   * Send an email informing the user their password has been updated.
   * 
   * @param account The account that was updated
   */
  @Async
  public void sendResetPassword(Account account) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:username", account.getUsername());
    send(account.getUsername(), "account-reset-password", formData);
  }

  /**
   * Send an email informing the user that an unrecognized device signed in to the
   * account.
   * 
   * @param account The account that was signed in
   * @param log     The security log containing request metadata of the request
   */
  @Async
  public void sendUnrecognizedDevice(Account account, SecurityLog log) {
    var metadata = log.getRequestMetadata();
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:time", log.getCreatedDate().toString());
    formData.add("v:cityName", metadata.getGeoMetadata().getCityName());
    formData.add("v:countryIsoCode", metadata.getGeoMetadata().getCountryIsoCode());
    formData.add("v:subdivisionIsoCode", metadata.getGeoMetadata().getSubdivisionIsoCode());
    formData.add("v:operatingSystemFamily", metadata.getDeviceMetadata().getOperatingSystemFamily());
    formData.add("v:userAgentFamily", metadata.getDeviceMetadata().getUserAgentFamily());
    send(account.getUsername(), "account-unrecognized-device", formData);
  }

  /**
   * Send an email informing the user a new key has been created.
   * 
   * @param account The account the key was created on
   */
  @Async
  public void sendKeyCreated(Account account, ApiKey createdKey) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:name", createdKey.getName());
    formData.add("v:prefix", createdKey.getPrefix());
    send(account.getUsername(), "key-created", formData);
  }

  /**
   * Send an email informing the user a key has been deleted.
   * 
   * @param account The account the key was deleted on
   */
  @Async
  public void sendKeyDeleted(Account account, ApiKey deletedKey) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:name", deletedKey.getName());
    formData.add("v:prefix", deletedKey.getPrefix());
    send(account.getUsername(), "key-deleted", formData);
  }

  private void send(String to, String templateName, MultiValueMap<String, String> formData) {
    var template = findEmailTemplate(templateName);
    log.info("email:send: to={}, template={}, formData={}", to, template, formData);
    var request = new HttpEntity<>(formData, null);
    restTemplate.postForEntity(messagesUri(to, template), request, String.class);
  }

  private URI messagesUri(String to, Template template) {
    return UriComponentsBuilder
        .fromHttpUrl(String.format(properties.getUrl(), properties.getDomain()).concat("/messages"))//
        .queryParam("from",
            String.format("%s <mailgun@%s.mailgun.org>", properties.getFrom().getName(), properties.getDomain()))//
        .queryParam("to", to)//
        .queryParam("subject", template.getSubject())//
        .queryParam("template", template.getName()).build().toUri();

  }

  private Template findEmailTemplate(String templateName) {
    return properties.getTemplates().stream().filter(t -> t.getName().equals(templateName)).findFirst()
        .orElseThrow(() -> new ApiException(CriticalCode.TEMPLATE_NOT_FOUND));
  }
}