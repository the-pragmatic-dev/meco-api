package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TextEndpointIT extends IntegrationData {

  @LocalServerPort
  private int port;

  // @formatter:off

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
  }

  // @endpoint:analyse

  @Test
  void shouldReturnTextAnalysisForAllTextScopes() {
    var request = textRequest();
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, apiKey())
      .contentType(JSON)
      .body(request)
    .when()
      .post(textEndpoint(port))
    .then()
        .body("languages", hasSize(1))
        .body("languages", hasItem("en"))
        .body("detected_languages", hasSize(1))
        .body("detected_languages", hasItem("en"))
        .rootPath("attribute_scores.toxicity")
          .body("span_scores", hasSize(1))
          .body("summary_score.value", greaterThan(0.8f))
        .rootPath("attribute_scores.severe_toxicity")
          .body("span_scores", hasSize(1))
          .body("summary_score.value", greaterThan(0.3f))
        .rootPath("attribute_scores.identity_attack")
          .body("span_scores", hasSize(1))
          .body("summary_score.value", greaterThan(0.1f))
        .rootPath("attribute_scores.insult")
          .body("span_scores", hasSize(1))
          .body("summary_score.value", greaterThan(0.8f))
        .rootPath("attribute_scores.profanity")
          .body("span_scores", hasSize(1))
          .body("summary_score.value", greaterThan(0.6f))
        .rootPath("attribute_scores.threat")
          .body("span_scores", hasSize(1))
          .body("summary_score.value", greaterThan(0.05f))
        .statusCode(200);
  }

  @Test
  void shouldReturnTextAnalysisForToxicityOnly() {
    // disable all but toxicity text scopes on api key
    var apiKeyUpdateRequest = apiKeyUpdateRequest();
    apiKeyUpdateRequest.setEnabled(true);
    apiKeyUpdateRequest.getScope().setText(textScopeRequest(true, false, false, false, false, false));
    given()
      .contentType(JSON)
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(port))
      .body(apiKeyUpdateRequest)
    .when()
      .put(apiKeyEndpoint(port) + "1")
      .then()
        .statusCode(200);
    // perform text request
    var request = textRequest();
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, apiKey())
      .contentType(JSON)
      .body(request)
    .when()
      .post(textEndpoint(port))
    .then()
        .body("languages", hasSize(1))
        .body("languages", hasItem("en"))
        .body("detected_languages", hasSize(1))
        .body("detected_languages", hasItem("en"))
        .body("attribute_scores.severe_toxicity", is(nullValue()))
        .body("attribute_scores.identity_attack", is(nullValue()))
        .body("attribute_scores.insult", is(nullValue()))
        .body("attribute_scores.profanity", is(nullValue()))
        .body("attribute_scores.threat", is(nullValue()))
        .rootPath("attribute_scores.toxicity")
          .body("span_scores", hasSize(1))
          .body("summary_score.value", greaterThan(0.8f))
        .statusCode(200);
  }

  // @formatter:on
}