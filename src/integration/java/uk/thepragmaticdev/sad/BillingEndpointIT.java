package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.exception.code.AuthCode;
import uk.thepragmaticdev.exception.code.BillingCode;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BillingEndpointIT extends IntegrationData {

  @LocalServerPort
  private int port;

  // @formatter:off

  /**
   * Called before each integration test to reset database to default state. 
   * A new account is created per test which contains a unique stipe test customer id.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
  }

  // @endpoint:create-subscription

  @Test
  void shouldNotCreateStripeCustomerIfCustomerAlreadyExists() {
    given()
    .headers(headers())
    .header(HttpHeaders.AUTHORIZATION, signin(port))
    .when()
      .post(billingEndpoint(port))
    .then()
        .body("status", is("CONFLICT"))
        .body("message", is(BillingCode.STRIPE_CREATE_CUSTOMER_CONFLICT.getMessage()))
        .statusCode(409);
  }

  @Test
  void shouldNotCreateSubscriptionWhenTokenIsInvalid() {
    given()
    .headers(headers())
    .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .contentType(JSON)
    .body(billingCreateSubscriptionRequest("price_invalid"))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotCreateSubscriptionForInvalidPrice() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest("price_invalid"))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_PRICE_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:cancel-subscription

  @Test
  void shouldNotCancelSubscriptionWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .delete(billingEndpoint(port) + "subscriptions")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotCancelSubscriptionIfNoSubscriptionExists() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(), port))
    .when()
      .delete(billingEndpoint(port) + "subscriptions")
    .then()
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @formatter:on
}