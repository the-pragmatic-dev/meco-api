package uk.thepragmaticdev.endpoint.controller;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.security.key.ApiKeyAuthenticationToken;
import uk.thepragmaticdev.text.TextService;
import uk.thepragmaticdev.text.dto.request.TextRequest;
import uk.thepragmaticdev.text.perspective.dto.response.AnalyseCommentResponse;

@RestController
@RequestMapping("v1/text")
@CrossOrigin("*")
public class TextController {

  private final TextService textService;

  /**
   * Endpoint for text analysis.
   * 
   * @param textService The service for analysing text requests
   */
  @Autowired
  public TextController(TextService textService) {
    this.textService = textService;
  }

  /**
   * Retrieve a detailed analysis of the given text by routing through perspective
   * api models. The level of detail depends on which text attributes are enabled.
   * 
   * @param token   An authentication token containing the authenticated api key
   * @param request The text to analyse
   * @return A detailed analysis of the given text
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AnalyseCommentResponse analyse(ApiKeyAuthenticationToken token, @Valid @RequestBody TextRequest request) {
    if (!(token.getPrincipal() instanceof ApiKey)) {
      throw new ApiException(CriticalCode.AUTHENTICATION_ERROR);
    }
    return textService.analyse(request.getText(), (ApiKey) token.getPrincipal());
  }
}