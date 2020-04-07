package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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
      .header(AUTH_HEADER, signin())
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
      .header(AUTH_HEADER, signin())
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
      .header(AUTH_HEADER, signin())
      .log().ifValidationFails()
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs")
    .then()
        .body("numberOfElements", is(3))
        .body("content", hasSize(3))
        .root("content[0]")
          .body("accountId", is(1))
          .body("action", is("subscription.created"))
          .body("amount", is("£0.00"))
          .body("instant", is("2020-02-25T15:55:19.111Z"))
        .root("content[1]")
          .body("accountId", is(1))
          .body("action", is("billing.invoice"))
          .body("amount", is("£0.00"))
          .body("instant", is("2020-02-25T15:50:19.111Z"))
        .root("content[2]")
          .body("accountId", is(1))
          .body("action", is("billing.paid"))
          .body("amount", is("-£50.00"))
          .body("instant", is("2020-02-25T15:40:19.111Z"))
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

  // @formatter:on
}