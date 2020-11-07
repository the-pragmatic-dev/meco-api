package uk.thepragmaticdev.text;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.ApiKeyCode;
import uk.thepragmaticdev.exception.code.TextCode;
import uk.thepragmaticdev.exception.handler.RestTemplateErrorHandler;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.kms.scope.TextScope;
import uk.thepragmaticdev.kms.usage.ApiKeyUsageCache;
import uk.thepragmaticdev.text.perspective.Attribute;
import uk.thepragmaticdev.text.perspective.Comment;
import uk.thepragmaticdev.text.perspective.RequestedAttributes;
import uk.thepragmaticdev.text.perspective.dto.request.AnalyseCommentRequest;
import uk.thepragmaticdev.text.perspective.dto.response.AnalyseCommentResponse;

@Service
public class TextService {

  private final TextProperties properties;

  private final RestTemplate restTemplate;

  private final ApiKeyUsageCache apiKeyUsageCache;

  /**
   * Service for analysing text requests. Requests are routed through perspective
   * api models.
   * 
   * @param properties       Properties for communicating with perspective
   * @param builder          The builder to create rest request
   * @param apiKeyUsageCache The service for tracking api key usage
   */
  @Autowired
  public TextService(TextProperties properties, RestTemplateBuilder builder, ApiKeyUsageCache apiKeyUsageCache) {
    this.properties = properties;
    this.restTemplate = builder.errorHandler(new RestTemplateErrorHandler()).build();
    this.apiKeyUsageCache = apiKeyUsageCache;
  }

  /**
   * Retrieve a detailed analysis of the given text by routing through perspective
   * api models. The level of detail depends on which text attributes are enabled.
   * Default to english only for MVP.
   * 
   * @param text   The text to analyse
   * @param apiKey The currently authenticated api key
   * @return A detailed analysis of the given text
   */
  public AnalyseCommentResponse analyse(String text, ApiKey apiKey) {
    if (!Boolean.TRUE.equals(apiKey.getEnabled())) {
      throw new ApiException(ApiKeyCode.API_KEY_DISABLED);
    }
    var usageCount = usageCount(apiKey.getScope().getTextScope());
    if (usageCount == 0) {
      throw new ApiException(TextCode.TEXT_DISABLED);
    }
    var request = new HttpEntity<>(createAnalyseCommentRequest(text, apiKey));
    var response = restTemplate.postForEntity(analyseUri(), request, AnalyseCommentResponse.class);
    apiKeyUsageCache.put(apiKey, usageCount, 0);
    return response.getBody();
  }

  private URI analyseUri() {
    return UriComponentsBuilder.fromHttpUrl(properties.getUrl() + "/comments:analyze")//
        .queryParam("key", properties.getSecretKey()).build().toUri();
  }

  private AnalyseCommentRequest createAnalyseCommentRequest(String text, ApiKey apiKey) {
    return new AnalyseCommentRequest(//
        new Comment(text), //
        List.of("en"), //
        createRequestedAttributes(apiKey));
  }

  private RequestedAttributes createRequestedAttributes(ApiKey apiKey) {
    var attributes = new RequestedAttributes();
    var textScope = apiKey.getScope().getTextScope();

    if (textScope.getToxicity()) {
      attributes.setToxicity(new Attribute());
    }
    if (textScope.getSevereToxicity()) {
      attributes.setSevereToxicity(new Attribute());
    }
    if (textScope.getIdentityAttack()) {
      attributes.setIdentityAttack(new Attribute());
    }
    if (textScope.getInsult()) {
      attributes.setInsult(new Attribute());
    }
    if (textScope.getProfanity()) {
      attributes.setProfanity(new Attribute());
    }
    if (textScope.getThreat()) {
      attributes.setThreat(new Attribute());
    }
    return attributes;
  }

  private int usageCount(TextScope textScope) {
    var scopes = List.of(textScope.getToxicity(), textScope.getSevereToxicity(), textScope.getIdentityAttack(),
        textScope.getInsult(), textScope.getProfanity(), textScope.getThreat());
    var count = 0;
    for (boolean scope : scopes) {
      count += (scope ? 1 : 0);
    }
    return count;
  }
}
