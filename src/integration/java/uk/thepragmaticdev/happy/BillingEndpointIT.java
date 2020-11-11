package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import com.stripe.exception.StripeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
import uk.thepragmaticdev.billing.dto.response.BillingPlanResponse;
import uk.thepragmaticdev.billing.dto.response.BillingResponse;
import uk.thepragmaticdev.billing.dto.response.InvoiceResponse;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BillingEndpointIT extends IntegrationData {

  @LocalServerPort
  private int port;

  @Autowired
  private AccountService accountService;

  // Only injected to test non public service endpoints
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

  // @endpoint:findAllPlans

  @Test
  void shouldReturnAllBillingPlansWithNoAuthentication() {
    given()
        .headers(headers())
        .when()
          .get(billingEndpoint(port) + "plans")
        .then()
            .body("$", hasSize(3))
            .rootPath("get(0)")
              .body("id", startsWith("price_"))
              .body("currency", is("gbp"))
              .body("nickname", is("starter"))
              .body("product", is("prod_starter"))
              .body("interval", is("month"))
              .body("interval_count", is(1))
              .body("tiers", is(nullValue()))
            .rootPath("get(1)")
              .body("id", startsWith("price_"))
              .body("currency", is("gbp"))
              .body("nickname", is("pro"))
              .body("product", is("prod_pro"))
              .body("interval", is("month"))
              .body("interval_count", is(1))
              .body("tiers", hasSize(2))
              .rootPath("get(1).tiers.get(0)")
                .body("flat_amount", is(20000))
                .body("unit_amount_decimal", is(0.0f))
                .body("up_to", is(100000))
              .rootPath("get(1).tiers.get(1)")
                .body("flat_amount", is(0))
                .body("unit_amount_decimal", is(0.1f))
                .body("up_to", is(0))
            .rootPath("get(2)")
              .body("id", startsWith("price_"))
              .body("currency", is("gbp"))
              .body("nickname", is("indie"))
              .body("product", is("prod_indie"))
              .body("interval", is("month"))
              .body("interval_count", is(1))
              .body("tiers", hasSize(2))
              .rootPath("get(2).tiers.get(0)")
                .body("flat_amount", is(5000))
                .body("unit_amount_decimal", is(0.0f))
                .body("up_to", is(10000))
              .rootPath("get(2).tiers.get(1)")
                .body("flat_amount", is(0))
                .body("unit_amount_decimal", is(0.2f))
                .body("up_to", is(0))
          .statusCode(200)
        .extract().body().jsonPath().getList(".", BillingPlanResponse.class);
  }

  // @endpoint:create-subscription

  @Test
  void shouldCreateSubscription() {
    // account created within @BeforeEach
    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_INDIE_PLAN))
        .when()
          .post(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", startsWith("cus_"))
          .body("subscription_id", startsWith("sub_"))
          .body("subscription_item_id", startsWith("si_"))
          .body("subscription_status", is("active"))
          .body("plan_id", startsWith("price_"))
          .body("plan_nickname", is("indie"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("updated_date", is(nullValue()))
          .statusCode(201)
        .extract().body().as(BillingResponse.class);

    var actualCreated = billingResponse.getCreatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedCreated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualCreated, expectedCreated, ChronoUnit.DAYS), is(true));
 
    var actualSubStart = billingResponse.getSubscriptionCurrentPeriodStart().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedSubStart = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualSubStart, expectedSubStart, ChronoUnit.DAYS), is(true));

    var actualSubEnd = billingResponse.getSubscriptionCurrentPeriodEnd().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedSubEnd = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(1).plusDays(3);
    assertThat(isSameDay(actualSubEnd, expectedSubEnd, ChronoUnit.DAYS), is(true));
  }

  // @endpoint:update-subscription

  @Test
  void shouldUpgradeSubscriptionFromStarterToIndie() {
    // account created within @BeforeEach
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_STARTER_PLAN);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_INDIE_PLAN))
        .when()
          .put(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("plan_id", is(TEST_INDIE_PLAN))
          .body("plan_nickname", is("indie"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    assertThat(billingResponse.getSubscriptionCurrentPeriodStart()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodStart()), is(true));
    assertThat(billingResponse.getSubscriptionCurrentPeriodEnd()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodEnd()), is(true));

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));

    var upcomingInvoice = billingService.findUpcomingInvoice(TEST_USERNAME);
    assertThat(upcomingInvoice.getTotal(), is(5000L));
  }

  @Test
  void shouldUpgradeSubscriptionFromStarterToPro() {
    // account created within @BeforeEach
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_STARTER_PLAN);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_PRO_PLAN))
        .when()
          .put(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("plan_id", is(TEST_PRO_PLAN))
          .body("plan_nickname", is("pro"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    assertThat(billingResponse.getSubscriptionCurrentPeriodStart()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodStart()), is(true));
    assertThat(billingResponse.getSubscriptionCurrentPeriodEnd()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodEnd()), is(true));

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));

    var upcomingInvoice = billingService.findUpcomingInvoice(TEST_USERNAME);
    assertThat(upcomingInvoice.getTotal(), is(20000L));
  }

  @Test
  void shouldUpgradeSubscriptionFromIndieToPro() {
    // account created within @BeforeEach
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_INDIE_PLAN);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_PRO_PLAN))
        .when()
          .put(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("subscription_current_period_start", is(originalBilling.getSubscriptionCurrentPeriodStart().toString()))
          .body("subscription_current_period_end", is(originalBilling.getSubscriptionCurrentPeriodEnd().toString()))
          .body("plan_id", is(TEST_PRO_PLAN))
          .body("plan_nickname", is("pro"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));
  }

  @Test
  void shouldUpgradeSubscriptionFromIndieToProAndPersistUsage() {
    // account created within @BeforeEach
    var operations = 200000;
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_INDIE_PLAN);
    billingService.createUsageRecord(TEST_USERNAME, operations);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_PRO_PLAN))
        .when()
          .put(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("subscription_current_period_start", is(originalBilling.getSubscriptionCurrentPeriodStart().toString()))
          .body("subscription_current_period_end", is(originalBilling.getSubscriptionCurrentPeriodEnd().toString()))
          .body("plan_id", is(TEST_PRO_PLAN))
          .body("plan_nickname", is("pro"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));

    var upcomingInvoice = billingService.findUpcomingInvoice(TEST_USERNAME);
    assertThat(upcomingInvoice.getTotal(), is(30000L));
  }

  @Test
  void shouldDowngradeSubscriptionFromProToIndie() {
    // account created within @BeforeEach
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_PRO_PLAN);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_INDIE_PLAN))
        .when()
          .put(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("subscription_current_period_start", is(originalBilling.getSubscriptionCurrentPeriodStart().toString()))
          .body("subscription_current_period_end", is(originalBilling.getSubscriptionCurrentPeriodEnd().toString()))
          .body("plan_id", is(TEST_INDIE_PLAN))
          .body("plan_nickname", is("indie"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));
  }

  @Test
  void shouldDowngradeSubscriptionFromProToIndieAndPersistUsage() {
    // account created within @BeforeEach
    var operations = 200000;
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_PRO_PLAN);
    billingService.createUsageRecord(TEST_USERNAME, operations);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_INDIE_PLAN))
        .when()
          .put(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("subscription_current_period_start", is(originalBilling.getSubscriptionCurrentPeriodStart().toString()))
          .body("subscription_current_period_end", is(originalBilling.getSubscriptionCurrentPeriodEnd().toString()))
          .body("plan_id", is(TEST_INDIE_PLAN))
          .body("plan_nickname", is("indie"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));

    var upcomingInvoice = billingService.findUpcomingInvoice(TEST_USERNAME);
    assertThat(upcomingInvoice.getTotal(), is(43000L));
  }

  @Test
  void shouldDowngradeSubscriptionFromIndieToStarterAndRefundUnusedOperations() {
    // account created within @BeforeEach
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_INDIE_PLAN);
    billingService.createUsageRecord(TEST_USERNAME, 5000);
    billingService.createUsageRecord(TEST_USERNAME, 2500);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_STARTER_PLAN))
        .when()
          .put(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("plan_id", is(TEST_STARTER_PLAN))
          .body("plan_nickname", is("starter"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    assertThat(billingResponse.getSubscriptionCurrentPeriodStart()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodStart()), is(true));
    assertThat(billingResponse.getSubscriptionCurrentPeriodEnd()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodEnd()), is(true));

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));

    var invoices = billingService.findAllInvoices(TEST_USERNAME);
    assertThat(invoices.size(), is(2));
    assertThat(invoices.get(0).getTotal(), is(3750L));
    assertThat(invoices.get(0).getItems().get(0).getAmount(), is(-1250L));
    assertThat(invoices.get(0).getItems().get(2).getAmount(), is(5000L));
    assertThat(invoices.get(1).getTotal(), is(0L));
  }

  @Test
  void shouldDowngradeSubscriptionFromProToStarterAndRefundUnusedOperations() {
    // account created within @BeforeEach
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_PRO_PLAN);
    billingService.createUsageRecord(TEST_USERNAME, 10000);
    billingService.createUsageRecord(TEST_USERNAME, 5000);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
          .body(billingCreateSubscriptionRequest(TEST_PAYMENT_METHOD, TEST_STARTER_PLAN))
        .when()
          .put(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("plan_id", is(TEST_STARTER_PLAN))
          .body("plan_nickname", is("starter"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    assertThat(billingResponse.getSubscriptionCurrentPeriodStart()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodStart()), is(true));
    assertThat(billingResponse.getSubscriptionCurrentPeriodEnd()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodEnd()), is(true));

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));

    var invoices = billingService.findAllInvoices(TEST_USERNAME);
    assertThat(invoices.size(), is(2));
    assertThat(invoices.get(0).getTotal(), is(3000L));
    assertThat(invoices.get(0).getItems().get(0).getAmount(), is(-17000L));
    assertThat(invoices.get(0).getItems().get(2).getAmount(), is(20000L));
    assertThat(invoices.get(1).getTotal(), is(0L));
  }

  // @endpoint:cancel-subscription

  @Test
  void shouldCancelIndieSubscriptionAndRefundUnusedOperations() {
    // account created within @BeforeEach
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_INDIE_PLAN);
    billingService.createUsageRecord(TEST_USERNAME, 5000);
    billingService.createUsageRecord(TEST_USERNAME, 2500);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
        .when()
          .delete(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("plan_id", is(TEST_STARTER_PLAN))
          .body("plan_nickname", is("starter"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    assertThat(billingResponse.getSubscriptionCurrentPeriodStart()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodStart()), is(true));
    assertThat(billingResponse.getSubscriptionCurrentPeriodEnd()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodEnd()), is(true));

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));

    var invoices = billingService.findAllInvoices(TEST_USERNAME);
    assertThat(invoices.size(), is(2));
    assertThat(invoices.get(0).getTotal(), is(3750L));
    assertThat(invoices.get(0).getItems().get(0).getAmount(), is(-1250L));
    assertThat(invoices.get(0).getItems().get(2).getAmount(), is(5000L));
    assertThat(invoices.get(1).getTotal(), is(0L));
  }

  @Test
  void shouldCancelProSubscriptionAndRefundUnusedOperations() {
    // account created within @BeforeEach
    var originalBilling = billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_PRO_PLAN);
    billingService.createUsageRecord(TEST_USERNAME, 10000);
    billingService.createUsageRecord(TEST_USERNAME, 5000);

    var billingResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
        .when()
          .delete(billingEndpoint(port) + "subscriptions")
        .then()
          .body("customer_id", is(originalBilling.getCustomerId()))
          .body("subscription_id", is(originalBilling.getSubscriptionId()))
          .body("subscription_item_id", is(originalBilling.getSubscriptionItemId()))
          .body("subscription_status", is("active"))
          .body("plan_id", is(TEST_STARTER_PLAN))
          .body("plan_nickname", is("starter"))
          .body("card_billing_name", is(nullValue()))
          .body("card_brand", is("visa"))
          .body("card_last4", is("4242"))
          .body("card_exp_month", is(11))
          .body("card_exp_year", is(2021))
          .body("created_date", is(originalBilling.getCreatedDate().toString()))
          .statusCode(200)
        .extract().body().as(BillingResponse.class);

    assertThat(billingResponse.getSubscriptionCurrentPeriodStart()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodStart()), is(true));
    assertThat(billingResponse.getSubscriptionCurrentPeriodEnd()
        .isAfter(originalBilling.getSubscriptionCurrentPeriodEnd()), is(true));

    var actualUpdated = billingResponse.getUpdatedDate().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedUpdated = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualUpdated, expectedUpdated, ChronoUnit.DAYS), is(true));

    var invoices = billingService.findAllInvoices(TEST_USERNAME);
    assertThat(invoices.size(), is(2));
    assertThat(invoices.get(0).getTotal(), is(3000L));
    assertThat(invoices.get(0).getItems().get(0).getAmount(), is(-17000L));
    assertThat(invoices.get(0).getItems().get(2).getAmount(), is(20000L));
    assertThat(invoices.get(1).getTotal(), is(0L));
  }

  // @endpoint:find-upcoming-invoice

  @Test
  void shouldReturnUpcomingInvoiceWithTwoLineItemsForZeroOverage() {
    // account created within @BeforeEach
    billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_INDIE_PLAN);

    var invoiceResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
        .when()
          .get(billingEndpoint(port) + "invoices/upcoming")
        .then()
          .body("number", is(emptyOrNullString()))
          .body("currency", is("gbp"))
          .body("subtotal", is(5000))
          .body("total", is(5000))
          .body("amount_due", is(5000))
          .body("items", hasSize(2))
          .rootPath("items.get(0)")
            .body("amount", is(0))
            .body("description", is("0 operation × Indie (Tier 1 at £0.00 / month)"))
          .rootPath("items.get(1)")
            .body("amount", is(5000))
            .body("description", is("Indie (Tier 1 at £50.00 / month)"))
          .statusCode(200)
        .extract().body().as(InvoiceResponse.class);

    var actualPeriodStart = invoiceResponse.getPeriodStart().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedPeriodStart = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualPeriodStart, expectedPeriodStart, ChronoUnit.DAYS), is(true));

    var actualPeriodEnd = invoiceResponse.getPeriodEnd().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedPeriodEnd = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(1);
    assertThat(isSameDay(actualPeriodEnd, expectedPeriodEnd, ChronoUnit.DAYS), is(true));
  }

  @Test
  void shouldReturnUpcomingInvoiceWithThreeLineItemsForOverages() {
    // account created within @BeforeEach
    var operations = 11000;
    billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_INDIE_PLAN);
    billingService.createUsageRecord(TEST_USERNAME, operations);
    
    var invoiceResponse = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin(authSigninRequest(TEST_USERNAME, TEST_PASSWORD), port))
          .contentType(JSON)
        .when()
          .get(billingEndpoint(port) + "invoices/upcoming")
        .then()
          .body("number", is(emptyOrNullString()))
          .body("currency", is("gbp"))
          .body("subtotal", is(5200))
          .body("total", is(5200))
          .body("amount_due", is(5200))
          .body("items", hasSize(3))
          .rootPath("items.get(0)")
            .body("amount", is(0))
            .body("description", is("10000 operation × Indie (Tier 1 at £0.00 / month)"))
          .rootPath("items.get(1)")
            .body("amount", is(5000))
            .body("description", is("Indie (Tier 1 at £50.00 / month)"))
          .rootPath("items.get(2)")
            .body("amount", is(200))
            .body("description", is("1000 operation × Indie (Tier 2 at £0.002 / month)"))
          .statusCode(200)
        .extract().body().as(InvoiceResponse.class);

    var actualPeriodStart = invoiceResponse.getPeriodStart().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedPeriodStart = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(isSameDay(actualPeriodStart, expectedPeriodStart, ChronoUnit.DAYS), is(true));

    var actualPeriodEnd = invoiceResponse.getPeriodEnd().withOffsetSameInstant(ZoneOffset.UTC);
    var expectedPeriodEnd = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(1);
    assertThat(isSameDay(actualPeriodEnd, expectedPeriodEnd, ChronoUnit.DAYS), is(true));
  }

  // @endpoint:internal->create-usage-record

  @Test
  void shouldCreateUsageRecordForSubscription() {
    // account created within @BeforeEach
    var operations = 10000;
    billingService.createSubscription(TEST_USERNAME, TEST_PAYMENT_METHOD, TEST_INDIE_PLAN);
    billingService.createUsageRecord(TEST_USERNAME, operations);
    var usageRecordSummaries = billingService.findAllUsageRecords(TEST_USERNAME).getData();
    assertThat(usageRecordSummaries, hasSize(1));
    assertThat(usageRecordSummaries.get(0).getTotalUsage(), is(10000L));
  }

  // @formatter:on
}