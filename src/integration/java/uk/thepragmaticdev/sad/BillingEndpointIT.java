package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import com.stripe.exception.StripeException;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.billing.BillingService;
import uk.thepragmaticdev.exception.code.AuthCode;
import uk.thepragmaticdev.exception.code.BillingCode;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BillingEndpointIT extends IntegrationData {

  @LocalServerPort
  private int port;

  @Autowired
  private AccountService accountService;

  // Only injected to delete customer
  @Autowired
  private BillingService billingService;

  // @formatter:off

  /**
   * Called before each integration test to reset database to default state. 
   * A new account is created per test which contains a unique stipe test customer id.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
    // Create a new account
    given()
      .headers(headers())
      .contentType(JSON)
      .body(authSignupRequest(TEST_USERNAME, TEST_PASSWORD))
    .when()
      .post(authEndpoint(port) + "signup")
    .then().statusCode(201);
    // Create a new Stripe customer for account
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
    .when()
      .post(billingEndpoint(port))
    .then()
        .body("customer_id", startsWith("cus_"))
        .statusCode(201);
  }

  /**
   * Clean up stripe customer test data.
   * 
   * @throws StripeException if a stripe api error occurs
   */
  @AfterEach
  public void cleanUpEach() throws StripeException {
    var account = accountService.findAuthenticatedAccount(TEST_USERNAME);
    billingService.deleteCustomer(account.getBilling().getCustomerId());
  }

  // @endpoint:create-customer

  @Test
  void shouldNotCreateStripeCustomerIfCustomerAlreadyExists() {
    given()
    .headers(headers())
    .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
    .when()
      .post(billingEndpoint(port))
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("CONFLICT"))
        .body("message", is(BillingCode.STRIPE_CREATE_CUSTOMER_CONFLICT.getMessage()))
        .statusCode(409);
  }

  @Test
  void shouldNotCreateStripeCustomerWhenTokenIsInvalid() {
    given()
    .headers(headers())
    .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .post(billingEndpoint(port))
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  // @endpoint:create-subscription

  @Test
  void shouldNotCreateSubscriptionWhenTokenIsInvalid() {
    given()
    .headers(headers())
    .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .contentType(JSON)
    .body(billingCreateSubscriptionRequest("payment_method_id", "plan"))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotCreateSubscriptionForNonExistingStripeCustomer() {
    var username = "billing.shouldNotCreateSubscriptionForNonExistingStripeCustomer@integration.test";
    var password = "password";
    // Create a new account with no stripe customer id
    given()
      .headers(headers())
      .contentType(JSON)
      .body(authSignupRequest(username, password))
    .when()
      .post(authEndpoint(port) + "signup")
    .then().statusCode(201);
    // Create subscription
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(username, password), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest("payment_method_id", "invalid_plan"))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_CUSTOMER_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  @Test
  void shouldNotCreateSubscriptionIfSubscriptionExists() {
    // create a valid subscription
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD_VISA, TEST_INDIE_PLAN))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .statusCode(201);
    // attempt to create another subscription
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD_VISA, TEST_INDIE_PLAN))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("CONFLICT"))
        .body("message", is(BillingCode.STRIPE_CREATE_SUBSCRIPTION_CONFLICT.getMessage()))
        .statusCode(409);
  }

  @Test
  void shouldNotCreateSubscriptionForInvalidPlan() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest("payment_method_id", "invalid_plan"))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_PLAN_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @endpoint:update-subscription

  @Test
  void shouldNotUpdateSubscriptionWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .put(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotUpdateSubscriptionForNonExistingStripeCustomer() {
    var username = "billing.shouldNotUpdateSubscriptionForNonExistingStripeCustomer@integration.test";
    var password = "password";
    // Create a new account with no stripe customer id
    given()
      .headers(headers())
      .contentType(JSON)
      .body(authSignupRequest(username, password))
    .when()
      .post(authEndpoint(port) + "signup")
    .then().statusCode(201);
    // Create subscription
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(username, password), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD_VISA, TEST_INDIE_PLAN))
    .when()
      .put(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_CUSTOMER_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  @Test
  void shouldNotUpdateSubscriptionIfNoSubscriptionExists() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD_VISA, TEST_INDIE_PLAN))
    .when()
      .put(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  @Test
  void shouldNotUpdateSubscriptionForInvalidPlan() {
    // create a valid subscription
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD_VISA, TEST_INDIE_PLAN))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .statusCode(201);
    // attempt to update to invalid plan
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD_VISA, "invalid_plan"))
    .when()
      .put(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_PLAN_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  @Test
  void shouldNotUpdateSubscriptionToSameActiveSubscription() {
    // create a valid subscription
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD_VISA, TEST_INDIE_PLAN))
    .when()
      .post(billingEndpoint(port) + "subscriptions")
    .then()
        .statusCode(201);
    // attempt to update to same active plan
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD_VISA, TEST_INDIE_PLAN))
    .when()
      .put(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("BAD_REQUEST"))
        .body("message", is(BillingCode.STRIPE_UPDATE_SUBSCRIPTION_INVALID.getMessage()))
        .statusCode(400);
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
        .body("id", is(not(emptyString())))
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotCancelSubscriptionForNonExistingStripeCustomer() {
    var username = "billing.shouldNotCancelSubscriptionForNonExistingStripeCustomer@integration.test";
    var password = "password";
    // Create a new account with no stripe customer id
    given()
      .headers(headers())
      .contentType(JSON)
      .body(authSignupRequest(username, password))
    .when()
      .post(authEndpoint(port) + "signup")
    .then().statusCode(201);
    // Create subscription
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(username, password), port))
    .when()
      .delete(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_CUSTOMER_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  @Test
  void shouldNotCancelSubscriptionIfNoSubscriptionExists() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
    .when()
      .delete(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  @Test
  void shouldNotCancelSubscriptionIfStarterSubscriptionIsActive() {
    billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD_VISA, TEST_STARTER_PLAN);

    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
    .when()
      .delete(billingEndpoint(port) + "subscriptions")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("BAD_REQUEST"))
        .body("message", is(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_INVALID.getMessage()))
        .statusCode(400);
  }

  // @endpoint:create-payment-method

  @Test
  void shouldNotCreatePaymentMethodWhenTokenIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
      .contentType(JSON)
      .body(billingCreatePaymentMethodRequest(TEST_PAYMENT_METHOD_MASTERCARD))
    .when()
      .post(billingEndpoint(port) + "cards")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotCreatePaymentMethodWhenIdIsNull() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreatePaymentMethodRequest(null))
    .when()
      .post(billingEndpoint(port) + "cards")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
            .body("object", is("billingCreatePaymentMethodRequest"))
            .body("field", is("paymentMethodId"))
            .body("rejected_value", is(nullValue()))
            .body("message", is("payment method id cannot be blank."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreatePaymentMethodWhenIdIsEmpty() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreatePaymentMethodRequest(""))
    .when()
      .post(billingEndpoint(port) + "cards")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("BAD_REQUEST"))
        .body("message", is("Validation errors"))
        .body("sub_errors", hasSize(1))
        .rootPath("sub_errors[0]")
            .body("object", is("billingCreatePaymentMethodRequest"))
            .body("field", is("paymentMethodId"))
            .body("rejected_value", is(""))
            .body("message", is("payment method id cannot be blank."))
        .statusCode(400);
  }

  @Test
  void shouldNotCreatePaymentMethodWhenIdIsInvalid() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
      .contentType(JSON)
      .body(billingCreatePaymentMethodRequest("invalid_id"))
    .when()
      .post(billingEndpoint(port) + "cards")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("SERVICE_UNAVAILABLE"))
        .body("message", is(BillingCode.STRIPE_CREATE_PAYMENT_METHOD_ERROR.getMessage()))
        .statusCode(503);
  }

  // @endpoint:find-upcoming-invoice

  @Test
  void shouldNotFindUpcomingInvoiceWhenTokenIsInvalid() {
    given()
    .headers(headers())
    .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN)
    .when()
      .get(billingEndpoint(port) + "invoices/upcoming")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("UNAUTHORIZED"))
        .body("message", is(AuthCode.ACCESS_TOKEN_INVALID.getMessage()))
        .statusCode(401);
  }

  @Test
  void shouldNotFindUpcomingInvoiceIfNoSubscriptionIsActive() {
    given()
    .headers(headers())
    .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
    .when()
      .get(billingEndpoint(port) + "invoices/upcoming")
    .then()
        .body("id", is(not(emptyString())))
        .body("status", is("NOT_FOUND"))
        .body("message", is(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_NOT_FOUND.getMessage()))
        .statusCode(404);
  }

  // @formatter:on
}