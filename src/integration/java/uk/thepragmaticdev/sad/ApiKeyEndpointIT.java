package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;
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
  void shouldNotReturnAllKeysWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT)
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:findById

  @Test
  void shouldNotReturnKeyWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotReturnUnknownKey() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "9999")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(ApiKeyCode.API_KEY_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:create

  @Test
  void shouldNotCreateKeyWhenNameIsEmpty() {
    var shortName = "";
    var request = apiKeyCreateRequest();
    request.setName(shortName);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("name"))
          .body("rejected_value", is(shortName))
          .body("message", is("API key name length must be between 1-50."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWhenNameIsTooLong() {
    var longName = IntStream.range(0, 51).mapToObj(i -> "a").collect(Collectors.joining(""));

    var request = apiKeyCreateRequest();
    request.setName(longName);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("name"))
          .body("rejected_value", is(longName))
          .body("message", is("API key name length must be between 1-50."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWhenNameIsNull() {
    var request = apiKeyCreateRequest();
    request.setName(null);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("name"))
          .body("rejected_value", is(nullValue()))
          .body("message", is("API key name cannot be null."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWhenRangeIsInvalid() {
    var request = apiKeyCreateRequest();
    var accessPolicy = accessPolicyRequest("name", "invalidRange");
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("accessPolicies"))
          .body("rejected_value", hasSize(1))
          .body("rejected_value[0].name", is(accessPolicy.getName()))
          .body("rejected_value[0].range", is(accessPolicy.getRange()))
          .body("message", is("Must match n.n.n.n/m where n=1-3 decimal digits, m = 1-3 decimal digits in range 1-32."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWhenRangeIsNull() {
    var request = apiKeyCreateRequest();
    var accessPolicy = accessPolicyRequest("name", null);
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("accessPolicies"))
          .body("rejected_value", hasSize(1))
          .body("rejected_value[0].name", is(accessPolicy.getName()))
          .body("rejected_value[0].range", is(accessPolicy.getRange()))
          .body("message", is("Must match n.n.n.n/m where n=1-3 decimal digits, m = 1-3 decimal digits in range 1-32."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWhenPolicyNameIsNull() {
    var request = apiKeyCreateRequest();
    var accessPolicy = accessPolicyRequest(null, "66.0.0.1/16");
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("accessPolicies[0].name"))
          .body("rejected_value", is(nullValue()))
          .body("message", is("Access policy name cannot be null."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWhenPolicyNameIsEmpty() {
    var request = apiKeyCreateRequest();
    var accessPolicy = accessPolicyRequest("", "66.0.0.1/16");
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("accessPolicies[0].name"))
          .body("rejected_value", is(""))
          .body("message", is("Access policy name length must be between 1-50."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateKeyWhenPolicyNameIsTooLong() {
    var request = apiKeyCreateRequest();
    var longName = IntStream.range(0, 51).mapToObj(i -> "a").collect(Collectors.joining(""));
    var accessPolicy = accessPolicyRequest(longName, "66.0.0.1/16");
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .post(API_KEY_ENDPOINT)
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyCreateRequest"))
          .body("field", is("accessPolicies[0].name"))
          .body("rejected_value", is(longName))
          .body("message", is("Access policy name length must be between 1-50."))
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
  void shouldNotUpdateKeyWhenTokenIsInvalid() {
    given()
      .contentType(JSON)
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
      .body(apiKeyUpdateRequest())
    .when()
      .put(API_KEY_ENDPOINT + "1")
      .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
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
        .body("message", is(ApiKeyCode.API_KEY_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  @Test
  void shouldNotUpdateKeyWhenNameIsEmpty() {
    var request = apiKeyUpdateRequest();
    request.setName("");

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyUpdateRequest"))
          .body("field", is("name"))
          .body("rejected_value", is(""))
          .body("message", is("API key name length must be between 1-50."))
        .statusCode(400);
  }

  @Test
  void shouldNotUpdateKeyWhenNameIsTooLong() {
    var longName = IntStream.range(0, 51).mapToObj(i -> "a").collect(Collectors.joining(""));

    var request = apiKeyUpdateRequest();
    request.setName(longName);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyUpdateRequest"))
          .body("field", is("name"))
          .body("rejected_value", is(longName))
          .body("message", is("API key name length must be between 1-50."))
        .statusCode(400);
  }

  @Test
  void shouldNotUpdateKeyWhenRangeIsInvalid() {
    var request = apiKeyUpdateRequest();
    var accessPolicy = accessPolicyRequest("name", "invalidRange");
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyUpdateRequest"))
          .body("field", is("accessPolicies"))
          .body("rejected_value", hasSize(1))
          .body("rejected_value[0].name", is(accessPolicy.getName()))
          .body("rejected_value[0].range", is(accessPolicy.getRange()))
          .body("message", is("Must match n.n.n.n/m where n=1-3 decimal digits, m = 1-3 decimal digits in range 1-32."))
        .statusCode(400);
  }

  @Test
  void shouldNotUpdateKeyWhenRangeIsNull() {
    var request = apiKeyUpdateRequest();
    var accessPolicy = accessPolicyRequest("name", null);
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyUpdateRequest"))
          .body("field", is("accessPolicies"))
          .body("rejected_value", hasSize(1))
          .body("rejected_value[0].name", is(accessPolicy.getName()))
          .body("rejected_value[0].range", is(accessPolicy.getRange()))
          .body("message", is("Must match n.n.n.n/m where n=1-3 decimal digits, m = 1-3 decimal digits in range 1-32."))
        .statusCode(400);
  }

  @Test
  void shouldNotUpdateKeyWhenPolicyNameIsNull() {
    var request = apiKeyUpdateRequest();
    var accessPolicy = accessPolicyRequest(null, "66.0.0.1/16");
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyUpdateRequest"))
          .body("field", is("accessPolicies[0].name"))
          .body("rejected_value", is(nullValue()))
          .body("message", is("Access policy name cannot be null."))
        .statusCode(400);
  }

  @Test
  void shouldNotUpdateKeyWhenPolicyNameIsEmpty() {
    var request = apiKeyUpdateRequest();
    var accessPolicy = accessPolicyRequest("", "66.0.0.1/16");
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyUpdateRequest"))
          .body("field", is("accessPolicies[0].name"))
          .body("rejected_value", is(""))
          .body("message", is("Access policy name length must be between 1-50."))
        .statusCode(400);
  }

  @Test
  void shouldNotUpdateKeyWhenPolicyNameIsTooLong() {
    var request = apiKeyUpdateRequest();
    var longName = IntStream.range(0, 51).mapToObj(i -> "a").collect(Collectors.joining(""));
    var accessPolicy = accessPolicyRequest(longName, "66.0.0.1/16");
    request.setAccessPolicies(List.of(accessPolicy));

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("apiKeyUpdateRequest"))
          .body("field", is("accessPolicies[0].name"))
          .body("rejected_value", is(longName))
          .body("message", is("Access policy name length must be between 1-50."))
        .statusCode(400);
  }

  // @endpoint:delete

  @Test
  void shouldNotDeleteKeyWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .delete(API_KEY_ENDPOINT + "1")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
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
        .body("message", is(ApiKeyCode.API_KEY_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:key-logs

  @Test
  void shouldNotReturnLatestKeyLogsWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT + "1/logs")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
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
        .body("message", is(ApiKeyCode.API_KEY_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:key-logs-download

  @Test
  void shouldNotDownloadKeyLogsWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT + "1/logs/download")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
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
        .body("message", is(ApiKeyCode.API_KEY_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:count

  @Test
  void shouldNotReturnKeyCountWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(API_KEY_ENDPOINT + "count")
      .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
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