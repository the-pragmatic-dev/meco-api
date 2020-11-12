package uk.thepragmaticdev.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.InvoiceItem;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Plan;
import com.stripe.model.PlanCollection;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.UsageRecord;
import com.stripe.model.UsageRecordSummaryCollection;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.InvoiceItemCreateParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.InvoiceUpcomingParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PlanListParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionRetrieveParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.UsageRecordCreateOnSubscriptionItemParams;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

  private final String stripeWebhookSignature;

  private RequestOptions requestOptions;

  /**
   * Service which wraps the stripe api static library and initialises secret key.
   * 
   * @param stripeSecretKey The secret key for communicating with stripe
   */
  public StripeService(@Value("${stripe.secret-key}") String stripeSecretKey,
      @Value("${stripe.webhook-signature}") String stripeWebhookSignature) {
    requestOptions = RequestOptions.builder().setApiKey(stripeSecretKey).build();
    this.stripeWebhookSignature = stripeWebhookSignature;
  }

  /**
   * Returns a list of plans.
   * 
   * @param active Only return plans that are active if true otherwise inactive
   * @return A list of plans
   * @throws StripeException On stripe related exceptions
   */
  public PlanCollection findAllPlans(boolean active) throws StripeException {
    return Plan.list(PlanListParams.builder().setActive(active).addExpand("data.tiers").build(), requestOptions);
  }

  /**
   * Creates a new customer object.
   * 
   * @param email       Customer’s email address
   * @param description An arbitrary string that you can attach to a customer
   *                    object
   * @return A new customer object.
   * @throws StripeException On stripe related exceptions
   */
  public Customer createCustomer(String email, String description) throws StripeException {
    return Customer.create(CustomerCreateParams.builder().setEmail(email).setDescription(description).build(),
        requestOptions);
  }

  /**
   * Permanently deletes a customer. It cannot be undone. Also immediately cancels
   * any active subscriptions on the customer.
   * 
   * @param customerId Customer's id
   * @return Deleted customer
   * @throws StripeException On stripe related exceptions
   */
  public Customer deleteCustomer(String customerId) throws StripeException {
    return Customer.retrieve(customerId, requestOptions).delete(requestOptions);
  }

  /**
   * Updates the specified customer by setting the values of the parameters
   * passed.
   * 
   * @param customerId             Customer's id
   * @param defaultPaymentMethodId ID of a payment method that’s attached to the
   *                               customer, to be used as the customer’s default
   *                               payment method for subscriptions and invoices.
   * @return Updated customer
   * @throws StripeException On stripe related exceptions
   */
  public Customer updateCustomer(String customerId, String defaultPaymentMethodId) throws StripeException {
    return Customer.retrieve(customerId, requestOptions)
        .update(CustomerUpdateParams.builder()
            .setInvoiceSettings(
                CustomerUpdateParams.InvoiceSettings.builder().setDefaultPaymentMethod(defaultPaymentMethodId).build())
            .build(), requestOptions);
  }

  /**
   * Creates a new subscription on an existing customer.
   * 
   * @param customerId Customer's id
   * @param priceId    The ID of the price object.
   * @return Created subscription
   * @throws StripeException On stripe related exceptions
   */
  public Subscription createSubscription(String customerId, String priceId) throws StripeException {
    return Subscription.create(SubscriptionCreateParams.builder().setCustomer(customerId)
        .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build()).build(), requestOptions);
  }

  /**
   * Retrieves the subscription with the given ID.
   * 
   * @param subscriptionId ID of the subscription
   * @return A subscription
   * @throws StripeException On stripe related exceptions
   */
  public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
    return Subscription.retrieve(subscriptionId,
        SubscriptionRetrieveParams.builder().addExpand("items.data.plan.tiers").build(), requestOptions);
  }

  /**
   * Updates an existing subscription on a customer to match the specified
   * parameters.
   * 
   * @param subscriptionId     ID of the subscription
   * @param cancelAtPeriodEnd  Boolean indicating whether this subscription should
   *                           cancel at the end of the current period.
   * @param billingCycleAnchor Either now or unchanged. Setting the value to now
   *                           resets the subscription's billing cycle anchor to
   *                           the current time.
   * @param prorationBehavior  Determines how to handle prorations when the
   *                           billing cycle changes.
   * @param subscriptionItemId Subscription item to update.
   * @param priceId            The ID of the price object.
   * @return Updated subscription
   * @throws StripeException On stripe related exceptions
   */
  public Subscription updateSubscription(String subscriptionId, boolean cancelAtPeriodEnd,
      SubscriptionUpdateParams.BillingCycleAnchor billingCycleAnchor,
      SubscriptionUpdateParams.ProrationBehavior prorationBehavior, String subscriptionItemId, String priceId)
      throws StripeException {
    return Subscription.retrieve(subscriptionId, requestOptions).update(SubscriptionUpdateParams.builder()//
        .setCancelAtPeriodEnd(cancelAtPeriodEnd)//
        .setBillingCycleAnchor(billingCycleAnchor)//
        .setProrationBehavior(prorationBehavior)//
        .addItem(SubscriptionUpdateParams.Item.builder().setId(subscriptionItemId).setPrice(priceId).build()).build(),
        requestOptions);
  }

  /**
   * Attaches a PaymentMethod object to a Customer.
   * 
   * @param paymentMethodId The ID of the payment method to attach to customer.
   * @param customerId      The ID of the customer to which to attach the
   *                        PaymentMethod.
   * @return The attached payment method
   * @throws StripeException On stripe related exceptions
   */
  public PaymentMethod attachPaymentMethod(String paymentMethodId, String customerId) throws StripeException {
    return PaymentMethod.retrieve(paymentMethodId, requestOptions)
        .attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build(), requestOptions);
  }

  /**
   * Creates a usage record for a specified subscription item and date, and fills
   * it with a quantity.
   * 
   * @param subscriptionItemId Subscription item to create usage on.
   * @param quantity           The usage quantity for the specified timestamp.
   * @param timestamp          The timestamp for the usage event.
   * @return The created usage record
   * @throws StripeException On stripe related exceptions
   */
  public UsageRecord createUsageRecord(String subscriptionItemId, long quantity, long timestamp)
      throws StripeException {
    return UsageRecord.createOnSubscriptionItem(subscriptionItemId,
        UsageRecordCreateOnSubscriptionItemParams.builder().setQuantity(quantity).setTimestamp(timestamp).build(),
        requestOptions);
  }

  /**
   * For the specified subscription item, returns a list of summary objects.
   * 
   * @param subscriptionItemId Subscription item to find usage records on.
   * @return A list of summary objects.
   * @throws StripeException On stripe related exceptions
   */
  public UsageRecordSummaryCollection findAllUsageRecords(String subscriptionItemId) throws StripeException {
    return SubscriptionItem.retrieve(subscriptionItemId, requestOptions).usageRecordSummaries(Map.of(), requestOptions);
  }

  /**
   * List invoices for a specific customer.
   * 
   * @param customerId Only return invoices for the customer specified by this
   *                   customer ID.
   * @return A list of invoices for customer.
   * @throws StripeException On stripe related exceptions
   */
  public InvoiceCollection findAllInvoices(String customerId) throws StripeException {
    return Invoice.list(InvoiceListParams.builder().setCustomer(customerId).build(), requestOptions);
  }

  /**
   * Preview the upcoming invoice for a customer.
   * 
   * @param customerId The identifier of the customer whose upcoming invoice you'd
   *                   like to retrieve.
   * @return An upcoming invoice.
   * @throws StripeException On stripe related exceptions
   */
  public Invoice findUpcomingInvoice(String customerId) throws StripeException {
    return Invoice.upcoming(InvoiceUpcomingParams.builder().setCustomer(customerId).build(), requestOptions);
  }

  /**
   * Creates an item to be added to a draft invoice (up to 250 items per invoice).
   * If no invoice is specified, the item will be on the next invoice created for
   * the customer specified.
   * 
   * @param customerId        The ID of the customer who will be billed when this
   *                          invoice item is billed.
   * @param description       An arbitrary string which you can attach to the
   *                          invoice item. The description is displayed in the
   *                          invoice for easy tracking.
   * @param currency          Three-letter ISO currency code, in lowercase. Must
   *                          be a supported currency.
   * @param quantity          Non-negative integer. The quantity of units for the
   *                          invoice item.
   * @param unitAmountDecimal Same as unit_amount, but accepts a decimal value
   *                          with at most 12 decimal places.
   * @return Created invoice item.
   * @throws StripeException On stripe related exceptions
   */
  public InvoiceItem createInvoiceItem(String customerId, String description, String currency, long quantity,
      double unitAmountDecimal) throws StripeException {
    return InvoiceItem
        .create(
            InvoiceItemCreateParams.builder().setCustomer(customerId).setDescription(description).setCurrency(currency)
                .setQuantity(quantity).setUnitAmountDecimal(BigDecimal.valueOf(unitAmountDecimal)).build(),
            requestOptions);
  }

  /**
   * Returns an event instance using the provided json payload.
   * 
   * @param payload   The payload sent by Stripe
   * @param sigHeader The contents of the signature header sent by Stripe
   * @return An event
   * @throws SignatureVerificationException if the signature verification fails
   */
  public Event constructEvent(String payload, String sigHeader) throws SignatureVerificationException {
    return Webhook.constructEvent(payload, sigHeader, stripeWebhookSignature);
  }
}