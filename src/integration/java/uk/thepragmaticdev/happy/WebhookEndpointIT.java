package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Plan;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
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

  // @endpoint:handleWebhookEvent->invoice.payment_succeeded

  @Test
  void shouldReturnOkWhenHandlingInvoicePaymentSucceededEvent() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_test");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_succeeded");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    var plan = mock(Plan.class);
    var subscriptionItem = mock(SubscriptionItem.class);
    when(subscriptionItem.getPlan()).thenReturn(plan);

    var subscriptionItemCollection = mock(SubscriptionItemCollection.class);
    when(subscriptionItemCollection.getData()).thenReturn(List.of(subscriptionItem));

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
            .statusCode(200);
  }

  @Test
  void shouldReturnOkWhenHandlingInvoicePaymentSucceededEventIfCurrentPeriodEndIsBeforeNow() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_test");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_succeeded");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    var subscription = mock(Subscription.class);
    when(subscription.getCurrentPeriodEnd()).thenReturn(OffsetDateTime.now().minusHours(1).toEpochSecond());
    doReturn(subscription).when(stripeService).retrieveSubscription(anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
            .statusCode(200);
  }

  // @endpoint:handleWebhookEvent->invoice.payment_failed

  @Test
  void shouldReturnOkWhenHandlingInvoicePaymentFailedEvent() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_test");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_failed");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    var plan = mock(Plan.class);
    var subscriptionItem = mock(SubscriptionItem.class);
    when(subscriptionItem.getPlan()).thenReturn(plan);

    var subscriptionItemCollection = mock(SubscriptionItemCollection.class);
    when(subscriptionItemCollection.getData()).thenReturn(List.of(subscriptionItem));

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
            .statusCode(200);
  }

  @Test
  void shouldReturnOkWhenHandlingInvoicePaymentFailedEventIfCurrentPeriodEndIsBeforeNow() throws StripeException {
    var invoice = mock(Invoice.class);
    when(invoice.getCustomer()).thenReturn("cus_test");
    when(invoice.getSubscription()).thenReturn("subscriptionId");

    var eventDataObjectDeserializer = mock(EventDataObjectDeserializer.class);
    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(invoice));

    var event = mock(Event.class);
    when(event.getType()).thenReturn("invoice.payment_failed");
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    var subscription = mock(Subscription.class);
    when(subscription.getCurrentPeriodEnd()).thenReturn(OffsetDateTime.now().minusHours(1).toEpochSecond());
    doReturn(subscription).when(stripeService).retrieveSubscription(anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
            .statusCode(200);
  }

  // @endpoint:handleWebhookEvent->customer.subscription.deleted

  @Test
  void shouldReturnOkWhenHandlingCustomerSubscriptionDeletedEvent() throws StripeException {
    var subscription = mock(Subscription.class);
    when(subscription.getCustomer()).thenReturn("cus_test");

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
            .statusCode(200);
  }

  // @endpoint:handleWebhookEvent->unsupported.event

  @Test
  void shouldReturnOkWhenHandlingUnsupportedEvent() throws StripeException {
    var event = mock(Event.class);
    when(event.getType()).thenReturn("unsupported.event");
    doReturn(event).when(stripeService).constructEvent(anyString(), anyString());

    given()
        .headers(headers())
        .header("stripe-signature", "signature")
        .contentType(JSON)
        .body("{}")
        .when()
          .post(webhookEndpoint(port) + "stripe")
        .then()
            .statusCode(200);
  }

  // @formatter:on
}