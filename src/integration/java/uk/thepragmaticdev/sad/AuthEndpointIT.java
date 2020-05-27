package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;
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
  void shouldNotSigninIfRequestMetadataIsMissing() {
    var request = authSigninRequest();
    given()
        .contentType(JSON)
        .body(request)
      .when()
        .post(AUTH_ENDPOINT + "signin")
      .then()
          .body("status", is("BAD_REQUEST"))
          .body("message", is(AuthCode.REQUEST_METADATA_INVALID.getMessage()))
          .statusCode(400);
  }

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
        .body("message", is(AuthCode.CREDENTIALS_INVALID.getMessage()))
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
        .body("message", is(AuthCode.CREDENTIALS_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:signup

  @Test
  void shouldNotSignupIfRequestMetadataIsMissing() {
    var request = authSignupRequest();
    request.setUsername("auth@integration.test");
    given()
        .contentType(JSON)
        .body(request)
      .when()
        .post(AUTH_ENDPOINT + "signup")
      .then()
          .body("status", is("BAD_REQUEST"))
          .body("message", is(AuthCode.REQUEST_METADATA_INVALID.getMessage()))
          .statusCode(400);
  }

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
        .body("status", is("NOT_FOUND"))
        .body("message", is(AuthCode.PASSWORD_RESET_TOKEN_NOT_FOUND.getMessage()))
        .statusCode(404);
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

  // @endpoint:refresh

  @Test
  void shouldNotRefreshIfAccessTokenHasNotExpired() {
    var request = authRefreshRequest(futureToken(), UUID.randomUUID().toString());

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("accessToken"))
          .body("rejectedValue", is(futureToken()))
          .body("message", is("Must be a valid, expired access token."))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfAccessTokenHasUnrecognizedSignature() {
    var request = authRefreshRequest(incorrectSignatureToken(), UUID.randomUUID().toString());

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("accessToken"))
          .body("rejectedValue", is(incorrectSignatureToken()))
          .body("message", is("Must be a valid, expired access token."))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfAccessTokenIsNull() {
    var request = authRefreshRequest(null, UUID.randomUUID().toString());

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("accessToken"))
          .body("rejectedValue", is(nullValue()))
          .body("message", is("Must be a valid, expired access token."))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfAccessTokenIsEmpty() {
    var request = authRefreshRequest("", UUID.randomUUID().toString());

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("accessToken"))
          .body("rejectedValue", is(""))
          .body("message", is("Must be a valid, expired access token."))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfRefreshTokenIsInvalid() {
    var refreshToken = "invalid";
    var request = authRefreshRequest(expiredToken(), refreshToken);

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("refreshToken"))
          .body("rejectedValue", is(refreshToken))
          .body("message", is("Must be a valid refresh token."))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfRefreshTokenIsNull() {
    var request = authRefreshRequest(expiredToken(), null);

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("refreshToken"))
          .body("rejectedValue", is(nullValue()))
          .body("message", is("Must be a valid refresh token."))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfRefreshTokenIsEmpty() {
    var request = authRefreshRequest(expiredToken(), "");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("subErrors", hasSize(1))
        .root("subErrors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("refreshToken"))
          .body("rejectedValue", is(""))
          .body("message", is("Must be a valid refresh token."))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfRefreshTokenHasExpired() {
    var request = authRefreshRequest(expiredToken(), "624aa34c-a00e-11ea-bb37-0242ac130002");
    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is(AuthCode.REFRESH_TOKEN_EXPIRED.getMessage()))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfRefreshTokenNotFound() {
    var request = authRefreshRequest(expiredToken(), UUID.randomUUID().toString());
    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(AuthCode.REFRESH_TOKEN_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  @Test
  void shouldNotRefreshIfRequestMetadataIsDifferent() {
    var request = authRefreshRequest(expiredToken(), "ee42cae2-a012-11ea-bb37-0242ac130002");
    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is(AuthCode.REQUEST_METADATA_INVALID.getMessage()))
        .statusCode(400);
  }

  @Test
  void shouldNotRefreshIfRequestMetadataIsMissing() {
    var request = authRefreshRequest(expiredToken(), "ee42cae2-a012-11ea-bb37-0242ac130002");
    given()
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is(AuthCode.REQUEST_METADATA_INVALID.getMessage()))
        .statusCode(400);
  }
}