package uk.thepragmaticdev.email;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.email.EmailProperties.Template;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.log.security.SecurityLog;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Service
public class EmailService {

  private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

  private static final String WEBCLIENT_INFO = "webclient:send: to={}, template={}, formData={}";

  private static final String WEBCLIENT_ERROR = "webclient:error: status={}, body={}";

  private final EmailProperties properties;

  private final WebClient webClient;

  /**
   * Service for sending emails. Provides default mailgun configuration.
   * 
   * @param properties       The email properties
   * @param webClientBuilder The web client for building request
   */
  public EmailService(EmailProperties properties, WebClient.Builder webClientBuilder) {
    this.properties = properties;
    this.webClient = webClientBuilder.baseUrl(String.format(properties.getUrl(), properties.getDomain()))//
        .defaultHeaders(header -> header.setBasicAuth("api", properties.getSecretKey()))//
        .build();
  }

  /**
   * Send a welcome email when a new account is created.
   * 
   * @param account The newly created account
   */
  public void sendAccountCreated(Account account) {
    send(account.getUsername(), "account-created", new LinkedMultiValueMap<>());
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
    send(account.getUsername(), "account-forgotten-password", formData);
  }

  /**
   * Send an email informing the user their password has been updated.
   * 
   * @param account The account that was updated
   */
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
  public void sendUnrecognizedDevice(Account account, SecurityLog log) {
    RequestMetadata metadata = log.getRequestMetadata();
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
  public void sendKeyDeleted(Account account, ApiKey deletedKey) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("v:name", deletedKey.getName());
    formData.add("v:prefix", deletedKey.getPrefix());
    send(account.getUsername(), "key-deleted", formData);
  }

  private void send(String to, String templateName, MultiValueMap<String, String> formData) {
    var template = findEmailTemplate(templateName);
    LOG.info(WEBCLIENT_INFO, to, template, formData);

    webClient.post().uri(uriBuilder -> messagesUri(uriBuilder, to, template))//
        .body(BodyInserters.fromFormData(formData)).retrieve()//
        .onStatus(HttpStatus::is4xxClientError, error -> {
          LOG.error(WEBCLIENT_ERROR, error.statusCode(), error.bodyToMono(String.class));
          return Mono.empty();
        })//
        .onStatus(HttpStatus::is5xxServerError, error -> {
          LOG.error(WEBCLIENT_ERROR, error.statusCode(), error.bodyToMono(String.class));
          return Mono.empty();
        })//
        .toBodilessEntity().subscribe();
  }

  private URI messagesUri(UriBuilder builder, String to, Template template) {
    return builder.path("/messages")//
        .queryParam("from",
            String.format("%s <mailgun@%s.mailgun.org>", properties.getFrom().getName(), properties.getDomain()))//
        .queryParam("to", to)//
        .queryParam("subject", template.getSubject())//
        .queryParam("template", template.getName())//
        .build();
  }

  private Template findEmailTemplate(String templateName) {
    return properties.getTemplates().stream().filter(t -> t.getName().equals(templateName)).findFirst()
        .orElseThrow(() -> new ApiException(CriticalCode.TEMPLATE_NOT_FOUND));
  }
}