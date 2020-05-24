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
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class BillingEndpointIT extends IntegrationData {

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
  void shouldNotCreateSubscriptionWithInvalidToken() {
    given()
    .headers(headers())
    .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .contentType(JSON)
    .body(billingCreateSubscriptionRequest("price_invalid"))
    .when()
      .post(BILLING_ENDPOINT + "subscriptions")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotCreateSubscriptionForInvalidPrice() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest()))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest("price_invalid"))
    .when()
      .post(BILLING_ENDPOINT + "subscriptions")
    .then()
        .body("status", is("SERVICE_UNAVAILABLE"))
        .body("message", is(BillingCode.STRIPE_PRICE_NOT_FOUND.getMessage()))
        .statusCode(503);
  }

  // @endpoint:cancel-subscription

  @Test
  void shouldNotCancelSubscriptionWithInvalidToken() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .delete(BILLING_ENDPOINT + "subscriptions")
    .then()
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.INVALID_EXPIRED_TOKEN.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotCancelSubscriptionIfNoSubscriptionExists() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest()))
    .when()
      .delete(BILLING_ENDPOINT + "subscriptions")
    .then()
        .body("status", is("SERVICE_UNAVAILABLE"))
        .body("message", is(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND.getMessage()))
        .statusCode(503);
  }

  // @formatter:on
}