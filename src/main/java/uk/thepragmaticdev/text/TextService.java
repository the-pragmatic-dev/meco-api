package uk.thepragmaticdev.text;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.text.perspective.Attribute;
import uk.thepragmaticdev.text.perspective.Comment;
import uk.thepragmaticdev.text.perspective.RequestedAttributes;
import uk.thepragmaticdev.text.perspective.dto.request.AnalyseCommentRequest;
import uk.thepragmaticdev.text.perspective.dto.response.AnalyseCommentResponse;

@Service
public class TextService {

  private final String perspectiveSecretKey;

  private final WebClient webClient;

  /**
   * Service for analysing text requests. Requests are routed through perspective
   * api models.
   * 
   * @param perspectiveSecretKey The secret key for communicating with perspective
   * @param webClientBuilder     The web client for building request
   */
  @Autowired
  public TextService(@Value("${perspective.secret-key}") String perspectiveSecretKey,
      WebClient.Builder webClientBuilder) {
    this.perspectiveSecretKey = perspectiveSecretKey;
    this.webClient = webClientBuilder.baseUrl(String.format("https://commentanalyzer.googleapis.com/v1alpha1/"))
        .build();
  }

  /**
   * Retrieve a detailed analysis of the given text by routing through perspective
   * api models. The level of detail depends on which text attributes are enabled.
   * 
   * @param text   The text to analyse
   * @param apiKey The currently authenticated api key
   * @return A detailed analysis of the given text
   */
  public Mono<AnalyseCommentResponse> analyse(String text, ApiKey apiKey) {
    return this.webClient.post() //
        .uri(uriBuilder -> uriBuilder //
            .path("/comments:analyze") //
            .queryParam("key", perspectiveSecretKey) //
            .build())
        .bodyValue(createAnalyseCommentRequest(text, apiKey)) //
        .retrieve().bodyToMono(AnalyseCommentResponse.class);
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
