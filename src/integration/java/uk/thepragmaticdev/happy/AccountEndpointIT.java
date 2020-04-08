package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.io.IOException;
import org.cactoos.io.ResourceOf;
import org.cactoos.text.FormattedText;
import org.cactoos.text.TextOf;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.account.Account;

@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class AccountEndpointIT extends IntegrationData {
  // @formatter:off

  @BeforeEach
  @FlywayTest
  public void initEach() throws Exception {
    new TextOf(
        new ResourceOf("data/billing.log.csv")
    ).asString();
  }

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

  @Test
  public void shouldReturnAuthenticatedAccount() {
    given()
      .header(AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("username", is("admin@email.com"))
        .body("fullName", is("Stephen Cathcart"))
        .body("emailSubscriptionEnabled", is(true))
        .body("billingAlertEnabled", is(false))
        .body("createdDate", is("2020-02-25T10:30:44.232Z"))
        .statusCode(200);
  }

  @Test
  public void shouldUpdateOnlyMutableAccountFields() {
    Account account = dirtyAccount();

    given()
      .contentType(JSON)
      .header(AUTHORIZATION, signin())
      .body(account)
    .when()
      .put(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("username", is("admin@email.com"))
        .body("fullName", is(account.getFullName()))
        .body("emailSubscriptionEnabled", is(account.isEmailSubscriptionEnabled()))
        .body("billingAlertEnabled", is(account.isBillingAlertEnabled()))
        .body("createdDate", is("2020-02-25T10:30:44.232Z"))
        .statusCode(200);
  }

  @Test
  public void shouldReturnLatestBillingLogs() {
    given()
      .header(AUTHORIZATION, signin())
      .log().ifValidationFails()
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

  @Test
  public void shouldReturnLatestSecurityLogs() {
    given()
      .header(AUTHORIZATION, signin())
      .log().ifValidationFails()
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

  @Test
  public void shouldDownloadBillingLogs() throws IOException {
    given()
      .header(AUTHORIZATION, signin())
      .log().ifValidationFails()
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs/download")
    .then()
        .header(ACCESS_CONTROL_EXPOSE_HEADERS, is("Content-Disposition"))
        .header(CONTENT_DISPOSITION, startsWith("attachment; filename="))
        .body(is(csv("data/billing.log.csv")))
        .statusCode(200);
  }

  @Test
  public void shouldDownloadSecurityLogs() throws IOException {
    given()
      .header(AUTHORIZATION, signin())
      .log().ifValidationFails()
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/logs/download")
    .then()
        .header(ACCESS_CONTROL_EXPOSE_HEADERS, is("Content-Disposition"))
        .header(CONTENT_DISPOSITION, startsWith("attachment; filename="))
        .body(is(csv("data/security.log.csv")))
        .statusCode(200);
  }

  private String signin() {
    return String.format("Bearer %s", 
      given()
        .contentType(JSON)
        .body(account())
      .when()
        .post(ACCOUNTS_ENDPOINT + "signin")
      .then()
          .body(not(emptyString()))
          .statusCode(200)
      .extract().body().asString());
  }

  private String csv(String file) throws IOException {
    return new FormattedText(new TextOf(
        new ResourceOf(file)
    )).asString();
  }

  // @formatter:on
}