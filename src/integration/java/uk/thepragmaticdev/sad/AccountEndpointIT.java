package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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
import uk.thepragmaticdev.exception.code.AuthCode;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class AccountEndpointIT extends IntegrationData {
  // @formatter:off

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() throws Exception {
  }

  // @endpoint:me

  @Test
  void shouldNotReturnAuthenticatedAccountWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:update

  @Test
  void shouldNotUpdateAccountWhenTokenIsInvalid() {
    var request = accountUpdateRequest();

    given()
      .contentType(JSON)
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
      .body(request)
    .when()
      .put(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotUpdateAccountWhenFullNameIsEmpty() {
    var request = accountUpdateRequest();
    request.setFullName("");

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .root("sub_errors[0]")
          .body("object", is("accountUpdateRequest"))
          .body("field", is("fullName"))
          .body("rejected_value", is(""))
          .body("message", is("Full name length must be between 1-50."))
        .statusCode(400);
  }

  @Test
  void shouldNotUpdateAccountWhenFullNameIsTooLong() {
    var longName = IntStream.range(0, 51).mapToObj(i -> "a").collect(Collectors.joining(""));

    var request = accountUpdateRequest();
    request.setFullName(longName);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .contentType(JSON)
      .body(request)
    .when()
      .put(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .root("sub_errors[0]")
          .body("object", is("accountUpdateRequest"))
          .body("field", is("fullName"))
          .body("rejected_value", is(longName))
          .body("message", is("Full name length must be between 1-50."))
        .statusCode(400);
  }

  // @endpoint:billing-logs

  @Test
  void shouldNotReturnLatestBillingLogsWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:billing-logs-download

  @Test
  void shouldNotDownloadBillingLogsWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs/download")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:security-logs

  @Test
  void shouldNotReturnLatestSecurityLogsWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/logs")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:security-logs-download

  @Test
  void shouldNotDownloadSecurityLogsWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/logs/download")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:find-all-active-devices

  @Test
  void shouldNotReturnAllActiveDevicesWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/devices")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:delete-all-active-devices

  @Test
  void shouldNotDeleteAllActiveDevicesWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
      .when()
        .delete(ACCOUNTS_ENDPOINT + "me/security/devices")
      .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @formatter:on
}