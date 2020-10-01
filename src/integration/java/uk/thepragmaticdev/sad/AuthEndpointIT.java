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
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.exception.code.AccountCode;
import uk.thepragmaticdev.exception.code.AuthCode;

@ActiveProfiles({ "async-disabled", "http-disabled", "prod" })
@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthEndpointIT extends IntegrationData {

  @LocalServerPort
  private int port;

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
        .post(authEndpoint(port) + "signin")
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
      .post(authEndpoint(port) + "signin")
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
      .post(authEndpoint(port) + "signin")
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
        .post(authEndpoint(port) + "signup")
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
      .post(authEndpoint(port) + "signup")
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
      .post(authEndpoint(port) + "signup")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authSignupRequest"))
          .body("field", is("username"))
          .body("rejected_value", is(request.getUsername()))
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
      .post(authEndpoint(port) + "signup")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authSignupRequest"))
          .body("field", is("password"))
          .body("rejected_value", is("[PROTECTED]"))
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
      .post(authEndpoint(port) + "forgot")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(AccountCode.USERNAME_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:reset

  @Test
  void shouldNotResetPasswordWhenTokenIsInvalid() {
    var request = authResetRequest();
    
    given()
      .headers(headers())
      .queryParam("token", "garbage")
      .contentType(JSON)
      .body(request)
    .when()
      .post(authEndpoint(port) + "reset")
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
      .post(authEndpoint(port) + "reset")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authResetRequest"))
          .body("field", is("password"))
          .body("rejected_value", is("[PROTECTED]"))
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
      .post(authEndpoint(port) + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("accessToken"))
          .body("rejected_value", is(futureToken()))
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
      .post(authEndpoint(port) + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("accessToken"))
          .body("rejected_value", is(incorrectSignatureToken()))
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
      .post(authEndpoint(port) + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("accessToken"))
          .body("rejected_value", is(nullValue()))
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
      .post(authEndpoint(port) + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("accessToken"))
          .body("rejected_value", is(""))
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
      .post(authEndpoint(port) + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("refreshToken"))
          .body("rejected_value", is(refreshToken))
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
      .post(authEndpoint(port) + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("refreshToken"))
          .body("rejected_value", is(nullValue()))
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
      .post(authEndpoint(port) + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
          .body("object", is("authRefreshRequest"))
          .body("field", is("refreshToken"))
          .body("rejected_value", is(""))
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
      .post(authEndpoint(port) + "refresh")
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
      .post(authEndpoint(port) + "refresh")
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
      .post(authEndpoint(port) + "refresh")
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
      .post(authEndpoint(port) + "refresh")
    .then()
        .body("status", is("BAD_REQUEST"))
        .body("message", is(AuthCode.REQUEST_METADATA_INVALID.getMessage()))
        .statusCode(400);
  }

  // @http:exception

  @Test
  void shouldReturnUnauthorizedWhenAuthorizationHeaderIsEmpty() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, "")
    .when()
      .get(accountEndpoint(port) + "me")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.AUTH_HEADER_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldReturnUnauthorizedWhenAuthorizationHeaderIsMissing() {
    given()
      .headers(headers())
    .when()
      .get(textEndpoint(port))
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.AUTH_HEADER_INVALID.getMessage()))
        .statusCode(401);
  }
}