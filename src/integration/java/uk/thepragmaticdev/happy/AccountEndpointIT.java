package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.IOException;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.account.Account;

@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class AccountEndpointIT extends IntegrationData {
  // @formatter:off

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
  }

  // @endpoint:signin

  @Test
  public String shouldSignin() {
    return signin();
  }

  // @endpoint:signup

  @Test
  public void shouldCreateAccount() {
    Account account = account();
    account.setUsername("test@email.com");

    given()
      .contentType(JSON)
      .body(account)
    .when()
      .post(ACCOUNTS_ENDPOINT + "signup")
    .then()
        .body(not(emptyString()))
        .statusCode(201);
  }

  // @endpoint:me

  @Test
  public void shouldReturnAuthenticatedAccount() {
    given()
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("id", is(nullValue()))
        .body("username", is("admin@email.com"))
        .body("password", is(nullValue()))
        .body("passwordResetToken", is(nullValue()))
        .body("fullName", is("Stephen Cathcart"))
        .body("emailSubscriptionEnabled", is(true))
        .body("billingAlertEnabled", is(false))
        .body("createdDate", is("2020-02-25T10:30:44.232Z"))
        .body("roles", is(nullValue()))
        .body("apiKeys", is(nullValue()))
        .statusCode(200);
  }

  // @endpoint:me/forgot

  @Test
  public void shouldReturnOkWhenForgottenPassword() {
    given()
      .params("username", account().getUsername())
    .when()
      .post(ACCOUNTS_ENDPOINT + "me/forgot")
    .then()
        .statusCode(200);
  }

  // @endpoint:me/reset

  // TODO hard as need to mock email service to get password reset token to send

  // @endpoint:update

  @Test
  public void shouldUpdateOnlyMutableAccountFields() {
    Account account = dirtyAccount();

    given()
      .contentType(JSON)
      .header(HttpHeaders.AUTHORIZATION, signin())
      .body(account)
    .when()
      .put(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("username", is("admin@email.com"))
        .body("password", is(nullValue()))
        .body("passwordResetToken", is(nullValue()))
        .body("fullName", is(account.getFullName()))
        .body("emailSubscriptionEnabled", is(account.getEmailSubscriptionEnabled()))
        .body("billingAlertEnabled", is(account.getBillingAlertEnabled()))
        .body("createdDate", is("2020-02-25T10:30:44.232Z"))
        .statusCode(200);
  }

  // @endpoint:billing-logs

  @Test
  public void shouldReturnLatestBillingLogs() {
    given()
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs")
    .then()
        .body("numberOfElements", is(3))
        .body("content", hasSize(3))
        .root("content[0]")
          .body("action", is("subscription.created"))
          .body("amount", is("£0.00"))
          .body("instant", is("2020-02-25T15:55:19.111Z"))
        .root("content[1]")
          .body("action", is("billing.invoice"))
          .body("amount", is("£0.00"))
          .body("instant", is("2020-02-25T15:50:19.111Z"))
        .root("content[2]")
          .body("action", is("billing.paid"))
          .body("amount", is("-£50.00"))
          .body("instant", is("2020-02-25T15:40:19.111Z"))
        .statusCode(200);
  }

  // @endpoint:billing-logs-download

  @Test
  public void shouldDownloadBillingLogs() throws IOException {
    given()
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs/download")
    .then()
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, is(HttpHeaders.CONTENT_DISPOSITION))
        .header(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename="))
        .body(is(csv("data/billing.log.csv")))
        .statusCode(200);
  }

  // @endpoint:security-logs

  @Test
  public void shouldReturnLatestSecurityLogs() {
    given()
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/logs")
    .then()
        .body("numberOfElements", is(3))
        .body("content", hasSize(3))
        .root("content[0]")
          .body("action", is("user.login"))
          .body("address", is("5.65.196.222"))
          .body("instant", is("2020-02-25T15:51:19.111Z"))
        .root("content[1]")
          .body("action", is("user.two_factor_successful_login"))
          .body("address", is("5.65.196.222"))
          .body("instant", is("2020-02-25T15:50:19.111Z"))
        .root("content[2]")
          .body("action", is("account.created"))
          .body("address", is("5.65.196.222"))
          .body("instant", is("2020-02-24T15:40:19.111Z"))
        .statusCode(200);
  }

  // @endpoint:security-logs-download

  @Test
  public void shouldDownloadSecurityLogs() throws IOException {
    given()
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/logs/download")
    .then()
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, is(HttpHeaders.CONTENT_DISPOSITION))
        .header(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename="))
        .body(is(csv("data/security.log.csv")))
        .statusCode(200);
  }

  // @formatter:on
}