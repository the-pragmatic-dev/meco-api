package uk.thepragmaticdev.text;

import java.net.URI;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.thepragmaticdev.exception.handler.RestTemplateErrorHandler;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.text.perspective.Attribute;
import uk.thepragmaticdev.text.perspective.Comment;
import uk.thepragmaticdev.text.perspective.RequestedAttributes;
import uk.thepragmaticdev.text.perspective.dto.request.AnalyseCommentRequest;
import uk.thepragmaticdev.text.perspective.dto.response.AnalyseCommentResponse;

@Log4j2
@Service
public class TextService {

  private final TextProperties properties;

  private final RestTemplate restTemplate;

  /**
   * Service for analysing text requests. Requests are routed through perspective
   * api models.
   * 
   * @param properties Properties for communicating with perspective
   * @param builder    The builder to create rest request
   */
  @Autowired
  public TextService(TextProperties properties, RestTemplateBuilder builder) {
    this.properties = properties;
    this.restTemplate = builder.errorHandler(new RestTemplateErrorHandler()).build();
  }

  /**
   * Retrieve a detailed analysis of the given text by routing through perspective
   * api models. The level of detail depends on which text attributes are enabled.
   * 
   * @param text   The text to analyse
   * @param apiKey The currently authenticated api key
   * @return A detailed analysis of the given text
   */
  public AnalyseCommentResponse analyse(String text, ApiKey apiKey) {
    var request = new HttpEntity<>(createAnalyseCommentRequest(text, apiKey));
    var response = restTemplate.postForEntity(analyseUri(), request, AnalyseCommentResponse.class);
    return response.getBody();
  }

  private URI analyseUri() {
    return UriComponentsBuilder.fromHttpUrl(properties.getUrl() + "/comments:analyze")//
        .queryParam("key", properties.getSecretKey()).build().toUri();
  }

  private AnalyseCommentRequest createAnalyseCommentRequest(String text, ApiKey apiKey) {
    return new AnalyseCommentRequest(//
        createComment(text), //
        createLanguages(), //
        createRequestedAttributes(apiKey));
  }

  private Comment createComment(String text) {
    return new Comment(text);
  }

  private List<String> createLanguages() {
    return List.of("en");
  }

  private RequestedAttributes createRequestedAttributes(ApiKey apiKey) {
    var attributes = new RequestedAttributes();
    // TODO dynamically add new attributes to only user enabled attributes from
    // api key
    attributes.setProfanity(new Attribute());
    attributes.setToxicity(new Attribute());
    return attributes;
  }
}
