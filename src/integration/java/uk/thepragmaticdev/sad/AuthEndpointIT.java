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
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.exception.code.AccountCode;
import uk.thepragmaticdev.exception.code.AuthCode;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class AuthEndpointIT extends IntegrationData {
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
  void shouldNotSigninWhenUsernameDoesNotExist() {
    var request = authSigninRequest();
    request.setUsername("random@email.com");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "signin")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_CREDENTIALS.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotSigninWhenPasswordIsInvalid() {
    var request = authSigninRequest();
    request.setPassword("invalidPassword");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "signin")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_CREDENTIALS.getMessage()))
        .statusCode(401);
  }

  // @endpoint:signup

  @Test
  void shouldNotCreateAccountWhenUsernameAlreadyExists() {
    var request = authSignupRequest();

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "signup")
    .then()
        .body("status", is("CONFLICT"))
        .body("message", is(AccountCode.USERNAME_UNAVAILABLE.getMessage()))
        .statusCode(409);
  }

  @Test
  void shouldNotCreateAccountWhenUsernameIsInvalidEmail() {
    var request = authSignupRequest();
    request.setUsername("invalid@");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "signup")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authSignupRequest"))
          .body("field", is("username"))
          .body("rejectedValue", is(request.getUsername()))
          .body("message", is("Username is not a valid email."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreateAccountWhenPasswordIsTooShort() {
    var request = authSignupRequest();
    request.setPassword("1234567");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "signup")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authSignupRequest"))
          .body("field", is("password"))
          .body("rejectedValue", is("[PROTECTED]"))
          .body("message", is("Minimum password length: 8 characters."))
        .statusCode(400);
  }

  // @endpoint:forgot

  @Test
  void shouldNotReturnOkWhenForgottenPasswordForUnknownUsername() {
    given()
      .headers(headers())
      .queryParam("username", "garbage@username.com")
    .when()
      .post(AUTH_ENDPOINT + "forgot")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(AccountCode.USERNAME_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:reset

  @Test
  void shouldNotResetPasswordWithInvalidToken() {
    var request = authResetRequest();
    
    given()
      .headers(headers())
      .queryParam("token", "garbage")
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "reset")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_PASSWORD_RESET_TOKEN.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotResetPasswordWhenPasswordIsTooShort() {
    var request = authResetRequest();
    request.setPassword("1234567");
    
    given()
      .headers(headers())
      .queryParam("token", "garbage")
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "reset")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authResetRequest"))
          .body("field", is("password"))
          .body("rejectedValue", is("[PROTECTED]"))
          .body("message", is("Minimum password length: 8 characters."))
        .statusCode(400);
  }  
}