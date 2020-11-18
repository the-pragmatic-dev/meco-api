package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.exception.code.ApiKeyCode;
import uk.thepragmaticdev.exception.code.AuthCode;
import uk.thepragmaticdev.exception.code.TextCode;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true" })
class TextEndpointIT extends IntegrationData {

  @LocalServerPort
  private int port;

  @Autowired
  private AccountService accountService;

  // @formatter:off

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() throws Exception {
  }

  // @endpoint:analyse

  @Test
  void shouldNotReturnTextResponseWhenApiKeyIsInvalid() {
    var request = textRequest();
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_API_KEY)
      .contentType(JSON)
      .body(request)
    .when()
      .post(textEndpoint(port))
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.API_KEY_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotReturnTextResponseWhenApiKeyIsFrozen() {
    // freeze account and api keys
    var account = accountService.findAuthenticatedAccount("admin@email.com");
    accountService.freeze(account);
    // perform text request
    var textRequest = textRequest();
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, apiKey())
      .contentType(JSON)
      .body(textRequest)
    .when()
      .post(textEndpoint(port))
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("PAYMENT_REQUIRED"))
        .body("message", is(ApiKeyCode.API_KEY_FROZEN.getMessage()))
        .statusCode(402);
  }

  @Test
  void shouldNotReturnTextResponseWhenApiKeyIsDisabled() {
    // disable api key
    var apiKeyUpdateRequest = apiKeyUpdateRequest();
    apiKeyUpdateRequest.setEnabled(false);
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
    var textRequest = textRequest();
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, apiKey())
      .contentType(JSON)
      .body(textRequest)
    .when()
      .post(textEndpoint(port))
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("FORBIDDEN"))
        .body("message", is(ApiKeyCode.API_KEY_DISABLED.getMessage()))
        .statusCode(403);
  }
  
  @Test
  void shouldNotReturnTextResponseWhenApiKeyHasNoTextScopesEnabled() {
    // disable all text scopes on api key
    var apiKeyUpdateRequest = apiKeyUpdateRequest();
    apiKeyUpdateRequest.setEnabled(true);
    apiKeyUpdateRequest.getScope().setText(textScopeRequest(false, false, false, false, false, false));
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
    var textRequest = textRequest();
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, apiKey())
      .contentType(JSON)
      .body(textRequest)
    .when()
      .post(textEndpoint(port))
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("FORBIDDEN"))
        .body("message", is(TextCode.TEXT_DISABLED.getMessage()))
        .statusCode(403);
  }
}
