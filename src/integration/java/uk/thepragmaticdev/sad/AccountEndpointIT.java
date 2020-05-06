package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.TestConfig;
import uk.thepragmaticdev.exception.code.AccountCode;

@Import(TestConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class AccountEndpointIT extends IntegrationData {
  // @formatter:off

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() throws Exception {
  }

  // @endpoint:signin

  @Test
  public void shouldNotSigninWhenUsernameDoesNotExist() {
    var request = accountSigninRequest();
    request.setUsername("random@email.com");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(ACCOUNTS_ENDPOINT + "signin")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AccountCode.INVALID_CREDENTIALS.getMessage()))
        .statusCode(401);
  }

  @Test
  public void shouldNotSigninWhenPasswordIsInvalid() {
    var request = accountSigninRequest();
    request.setPassword("invalidPassword");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(ACCOUNTS_ENDPOINT + "signin")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AccountCode.INVALID_CREDENTIALS.getMessage()))
        .statusCode(401);
  }

  // @endpoint:signup

  @Test
  public void shouldNotCreateAccountWhenUsernameAlreadyExists() {
    var request = accountSignupRequest();

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(ACCOUNTS_ENDPOINT + "signup")
    .then()
        .body("status", is("CONFLICT"))
        .body("message", is(AccountCode.USERNAME_UNAVAILABLE.getMessage()))
        .statusCode(409);
  }

  @Test
  public void shouldNotCreateAccountWhenUsernameIsInvalidEmail() {
    var request = accountSignupRequest();
    request.setUsername("invalid@");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(ACCOUNTS_ENDPOINT + "signup")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("accountSignupRequest"))
          .body("field", is("username"))
          .body("rejectedValue", is(request.getUsername()))
          .body("message", is("Username is not a valid email."))
        .statusCode(400);
  }

  @Test
  public void shouldNotCreateAccountWhenPasswordIsTooShort() {
    var request = accountSignupRequest();
    request.setPassword("1234567");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(ACCOUNTS_ENDPOINT + "signup")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("accountSignupRequest"))
          .body("field", is("password"))
          .body("rejectedValue", is("[PROTECTED]"))
          .body("message", is("Minimum password length: 8 characters."))
        .statusCode(400);
  }

  // @endpoint:me

  @Test
  public void shouldNotReturnAuthenticatedAccountWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AccountCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  // @endpoint:me/forgot

  @Test
  public void shouldNotReturnOkWhenForgottenPasswordForUnknownUsername() {
    given()
      .headers(headers())
      .queryParam("username", "garbage@username.com")
    .when()
      .post(ACCOUNTS_ENDPOINT + "me/forgot")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(AccountCode.USERNAME_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:me/reset

  @Test
  public void shouldNotResetPasswordWithInvalidToken() {
    var request = accountResetRequest();
    
    given()
      .headers(headers())
      .queryParam("token", "garbage")
      .contentType(JSON)
      .body(request)
    .when()
      .post(ACCOUNTS_ENDPOINT + "me/reset")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AccountCode.INVALID_PASSWORD_RESET_TOKEN.getMessage()))
        .statusCode(401);
  }

  @Test
  public void shouldNotResetPasswordWhenPasswordIsTooShort() {
    var request = accountResetRequest();
    request.setPassword("1234567");
    
    given()
      .headers(headers())
      .queryParam("token", "garbage")
      .contentType(JSON)
      .body(request)
    .when()
      .post(ACCOUNTS_ENDPOINT + "me/reset")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("accountResetRequest"))
          .body("field", is("password"))
          .body("rejectedValue", is("[PROTECTED]"))
          .body("message", is("Minimum password length: 8 characters."))
        .statusCode(400);
  }

  // @endpoint:update

  @Test
  public void shouldNotUpdateAccountWithInvalidToken() {
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
        .body("message", is(AccountCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  // @endpoint:billing-logs

  @Test
  public void shouldNotReturnLatestBillingLogsWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AccountCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  // @endpoint:billing-logs-download

  @Test
  public void shouldNotDownloadBillingLogsWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs/download")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AccountCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  // @endpoint:security-logs

  @Test
  public void shouldNotReturnLatestSecurityLogsWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/logs")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AccountCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  // @endpoint:security-logs-download

  @Test
  public void shouldNotDownloadSecurityLogsWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/logs/download")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AccountCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  // @formatter:on
}