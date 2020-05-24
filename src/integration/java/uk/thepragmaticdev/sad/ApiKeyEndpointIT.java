package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.IntStream;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.exception.code.ApiKeyCode;
import uk.thepragmaticdev.exception.code.AuthCode;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class ApiKeyEndpointIT extends IntegrationData {
  // @formatter:off

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() throws Exception {
  }

  // @endpoint:findAll

  @Test
  void shouldNotReturnAllKeysWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT)
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  // @endpoint:create

  @Test
  void shouldNotCreateKeyWhenNameIsTooShort() {
    var shortName = "ab";
    var key = apiKeyCreateRequest();
    key.setName(shortName);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(key)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("name"))
          .body("rejectedValue", is(shortName))
          .body("message", is("size must be between 3 and 20"))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWhenNameIsTooLong() {
    var longName = "abcdefghijklmnopqrstu";
    var key = apiKeyCreateRequest();
    key.setName(longName);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(key)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("name"))
          .body("rejectedValue", is(longName))
          .body("message", is("size must be between 3 and 20"))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWithNoScope() {
    var key = apiKeyCreateRequest();
    key.setScope(null);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(key)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("scope"))
          .body("rejectedValue", is(nullValue()))
          .body("message", is("must not be null"))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWithInvalidIpv4Cidr() {
    var key = apiKeyCreateRequest();
    var accessPolicy = accessPolicyRequest("name", "invalidRange");
    key.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(key)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("accessPolicies"))
          .body("rejectedValue", hasSize(1))
          .body("rejectedValue[0].name", is(accessPolicy.getName()))
          .body("rejectedValue[0].range", is(accessPolicy.getRange()))
          .body("message", is("Must match n.n.n.n/m where n=1-3 decimal digits, m = 1-3 decimal digits in range 1-32."))
        .statusCode(400);
  } 

  @Test
  void shouldNotCreateKeyWhenAtMaxKeyLimit() {
    assertKeyCount(2);
    IntStream.range(0, 8).forEach(i -> createValidKey());
    assertKeyCount(10);
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(apiKeyCreateRequest())
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("FORBIDDEN"))
        .body("message", is(ApiKeyCode.API_KEY_LIMIT.getMessage()))
        .statusCode(403);
  }

  // @endpoint:update

  @Test
  void shouldNotUpdateKeyWithInvalidToken() {
    given()
      .contentType(JSON)
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
      .body(apiKeyUpdateRequest())
    .when()
      .put(API_KEY_ENDPOINT + "1")
      .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }
  
  @Test
  void shouldNotUpdateUnknownKey() {
    given()
      .contentType(JSON)
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .body(apiKeyUpdateRequest())
    .when()
      .put(API_KEY_ENDPOINT + "9999")
      .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(ApiKeyCode.NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:delete

  @Test
  void shouldNotDeleteKeyWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .delete(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotDeleteUnknownKey() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .delete(API_KEY_ENDPOINT + "9999")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(ApiKeyCode.NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:key-logs

  @Test
  void shouldNotReturnLatestKeyLogsWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT + "1/logs")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotReturnLatestKeyLogsForUnknownKey() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "9999/logs")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(ApiKeyCode.NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:key-logs-download

  @Test
  void shouldNotDownloadKeyLogsWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT + "1/logs/download")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotDownloadKeyLogsForUnknownKey() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "9999/logs/download")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(ApiKeyCode.NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:count

  @Test
  void shouldNotReturnKeyCountWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT + "count")
      .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  private void createValidKey() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(apiKeyCreateRequest())
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .statusCode(201);
  }

  private void assertKeyCount(int expectedCount) {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "count")
      .then()
        .body(is(Integer.toString(expectedCount)))
        .statusCode(200);
  }

  // @formatter:on
}