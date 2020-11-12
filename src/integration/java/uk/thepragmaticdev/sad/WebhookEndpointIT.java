package uk.thepragmaticdev.sad;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonSyntaxException;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItemCollection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.billing.StripeService;
import uk.thepragmaticdev.exception.code.BillingCode;
import uk.thepragmaticdev.exception.code.WebhookCode;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WebhookEndpointIT extends IntegrationData {

  @LocalServerPort
  private int port;

  @Autowired
  private StripeService stripeService;

  // @formatter:off

  /**
   * Called before each integration test to reset database to default state. 
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
  }

  @AfterEach
  public void afterEach() {
    Mockito.reset(stripeService);
  }

  // @endpoint:handleWebhookEvent

  @Test
  void shouldNotReturnOkOnSerialisationError() throws StripeException {
    doThrow(JsonSyntaxException.class).when(stripeService).constructEvent(anyString(), anyString());
    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("INTERNAL_SERVER_ERROR"))
          .body("message", is(WebhookCode.DESERIALIIZATION_ERROR.getMessage()))
          .statusCode(500);
  }

  @Test
  void shouldNotReturnOkOnSignatureVerificationError() throws StripeException {
    doThrow(SignatureVerificationException.class).when(stripeService).constructEvent(anyString(), anyString());
    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("INTERNAL_SERVER_ERROR"))
          .body("message", is(WebhookCode.SIGNATURE_VERIFICATION_ERROR.getMessage()))
          .statusCode(500);
  }

  @Test
  void shouldNotReturnOkOnSyncError() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_test");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_succeeded");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    doThrow(ApiConnectionException.class).when(stripeService).retrieveSubscription(anyString());
    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("SERVICE_UNAVAILABLE"))
          .body("message", is(BillingCode.STRIPE_SYNC_SUBSCRIPTION_ERROR.getMessage()))
          .statusCode(503);
  }

  // @endpoint:handleWebhookEvent>invoice.payment_succeeded

  @Test
  void shouldNotReturnOkWhenHandlingInvoicePaymentSucceededEventIfObjectIsMissing() throws StripeException {
    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.empty());

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_succeeded");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(WebhookCode.OBJECT_MISSING_ERROR.getMessage()))
          .statusCode(404);
  }

  @Test
  void shouldNotReturnOkWhenHandlingInvoicePaymentSucceededEventIfCustomerIsMissing() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn(null);

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_succeeded");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(BillingCode.STRIPE_CUSTOMER_NOT_FOUND.getMessage()))
          .statusCode(404);
  }

  @Test
  void shouldNotReturnOkWhenHandlingInvoicePaymentSucceededEventIfBillingIsMissing() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_invalid");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_succeeded");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());
    doReturn(null).when(stripeService).retrieveSubscription(anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(BillingCode.BILLING_NOT_FOUND.getMessage()))
          .statusCode(404);
  }

  @Test
  void shouldNotReturnOkWhenHandlingInvoicePaymentSucceededEventIfSubscriptionItemIsMissing() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_test");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_succeeded");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    var subscriptionItemCollection = mock(SubscriptionItemCollection.class);
    when(subscriptionItemCollection.getData()).thenReturn(List.of());

    var subscription = mock(Subscription.class);
    when(subscription.getCurrentPeriodEnd()).thenReturn(OffsetDateTime.now().plusHours(1).toEpochSecond());
    when(subscription.getItems()).thenReturn(subscriptionItemCollection);
    doReturn(subscription).when(stripeService).retrieveSubscription(anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(BillingCode.STRIPE_SUBSCRIPTION_ITEM_NOT_FOUND.getMessage()))
          .statusCode(404);
  }

  // @endpoint:handleWebhookEvent>invoice.payment_failed

  @Test
  void shouldNotReturnOkWhenHandlingInvoicePaymentFailedEventIfObjectIsMissing() throws StripeException {
    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.empty());

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_failed");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(WebhookCode.OBJECT_MISSING_ERROR.getMessage()))
          .statusCode(404);
  }

  @Test
  void shouldNotReturnOkWhenHandlingInvoicePaymentFailedEventIfCustomerIsMissing() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn(null);

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_failed");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(BillingCode.STRIPE_CUSTOMER_NOT_FOUND.getMessage()))
          .statusCode(404);
  }

  @Test
  void shouldNotReturnOkWhenHandlingInvoicePaymentFailedEventIfBillingIsMissing() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_invalid");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_failed");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());
    doReturn(null).when(stripeService).retrieveSubscription(anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(BillingCode.BILLING_NOT_FOUND.getMessage()))
          .statusCode(404);
  }

  @Test
  void shouldNotReturnOkWhenHandlingInvoicePaymentFailedEventIfSubscriptionItemIsMissing() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_test");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_failed");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    var subscriptionItemCollection = mock(SubscriptionItemCollection.class);
    when(subscriptionItemCollection.getData()).thenReturn(List.of());

    var subscription = mock(Subscription.class);
    when(subscription.getCurrentPeriodEnd()).thenReturn(OffsetDateTime.now().plusHours(1).toEpochSecond());
    when(subscription.getItems()).thenReturn(subscriptionItemCollection);
    doReturn(subscription).when(stripeService).retrieveSubscription(anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(BillingCode.STRIPE_SUBSCRIPTION_ITEM_NOT_FOUND.getMessage()))
          .statusCode(404);
  }

  // @endpoint:handleWebhookEvent>customer.subscription.deleted

  @Test
  void shouldNotReturnOkWhenHandlingCustomerSubscriptionDeletedEventIfObjectIsMissing() throws StripeException {
    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.empty());

    var event = mock(Event.class);
    when(event.getType()).thenReturn("customer.subscription.deleted");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(WebhookCode.OBJECT_MISSING_ERROR.getMessage()))
          .statusCode(404);
  }

  @Test
  void shouldNotReturnOkWhenHandlingCustomerSubscriptionDeletedEventIfCustomerIsMissing() throws StripeException {
    var subscription = mock(Subscription.class);
    when(subscription.getCustomer()).thenReturn(null);

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(subscription));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("customer.subscription.deleted");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(BillingCode.STRIPE_CUSTOMER_NOT_FOUND.getMessage()))
          .statusCode(404);
  }

  @Test
  void shouldNotReturnOkWhenHandlingCustomerSubscriptionDeletedEventIfBillingIsMissing() throws StripeException {
    var subscription = mock(Subscription.class);
    when(subscription.getCustomer()).thenReturn("cus_invalid");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(subscription));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("customer.subscription.deleted");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
          .body("id", is(not(emptyString())))
          .body("status", is("NOT_FOUND"))
          .body("message", is(BillingCode.BILLING_NOT_FOUND.getMessage()))
          .statusCode(404);
  }
  
  // @formatter:on
}