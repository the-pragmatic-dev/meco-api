package uk.thepragmaticdev.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PlanCollection;
import com.stripe.model.Subscription;
import com.stripe.model.UsageRecordSummaryCollection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.billing.dto.response.InvoiceLineItemResponse;
import uk.thepragmaticdev.billing.dto.response.InvoiceResponse;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.BillingCode;

@Service
public class BillingService {

  private final BillingRepository billingRepository;

  private final AccountService accountService;

  private final StripeService stripeService;

  private static final String BILLING_CYCLE_NOW = "now";

  private static final String BILLING_CYCLE_UNCHANGED = "unchanged";

  private static final String KEY_ACTIVE = "active";

  private static final String KEY_BILLING_CYCLE_ANCHOR = "billing_cycle_anchor";

  private static final String KEY_CANCEL_AT_PERIOD_END = "cancel_at_period_end";

  private static final String KEY_CURRENCY = "currency";

  private static final String KEY_CUSTOMER = "customer";

  private static final String KEY_DEFAULT_PAYMENT_METHOD = "default_payment_method";

  private static final String KEY_DESCRIPTION = "description";

  private static final String KEY_EMAIL = "email";

  private static final String KEY_INVOICE_SETTINGS = "invoice_settings";

  private static final String KEY_ITEMS = "items";

  private static final String KEY_ITEM_0_ID = "items[0][id]";

  private static final String KEY_ITEM_0_PRICE = "items[0][price]";

  private static final String KEY_METADATA = "metadata";

  private static final String KEY_PRICE = "price";

  private static final String KEY_PRORATION_BEHAVIOUR = "proration_behavior";

  private static final String KEY_QUANTITY = "quantity";

  private static final String KEY_TIMESTAMP = "timestamp";

  private static final String KEY_UNIT_AMOUNT_DECIMAL = "unit_amount_decimal";

  private static final String KEY_USERNAME = "username";

  private static final String PLAN_STARTER = "starter";

  private static final String PLAN_INDIE = "indie";

  private static final String PLAN_PRO = "pro";

  private static final String PRORATION_BEHAVIOUR_NONE = "none";

  private static final String PRORATION_BEHAVIOUR_ALWAYS = "always_invoice";

  private static final String STRIPE_EXCEPTION_NO_INVOICE = "invoice_upcoming_none";

  /**
   * Service for creating stripe customers and handling subscriptions to non-free
   * prices.
   * 
   * @param billingRepository The data access repository for billing
   * @param accountService    The service for retrieving account information
   * @param stripeService     The service containing the stripe api
   */
  public BillingService(//
      BillingRepository billingRepository, //
      @Lazy AccountService accountService, //
      StripeService stripeService) {
    this.billingRepository = billingRepository;
    this.accountService = accountService;
    this.stripeService = stripeService;
  }

  /**
   * Find all active plans held by stripe.
   * 
   * @return A list of all active plans held by stripe
   */
  public PlanCollection findAllPlans() {
    Map<String, Object> params = Map.of(KEY_ACTIVE, true);
    try {
      return stripeService.findAllPlans(params);
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_FIND_ALL_PLANS_ERROR);
    }
  }

  /**
   * Create a new stripe customer for an authenticated account.
   * 
   * @param username The authenticated account username
   * @return The customer billing information
   */
  public Billing createCustomer(String username) {
    var account = accountService.findAuthenticatedAccount(username);
    if (!StringUtils.isBlank(account.getBilling().getCustomerId())) {
      throw new ApiException(BillingCode.STRIPE_CREATE_CUSTOMER_CONFLICT);
    }
    var params = Map.of(KEY_EMAIL, account.getUsername(), //
        KEY_DESCRIPTION, "MECO Account", //
        KEY_METADATA, Map.of(KEY_USERNAME, account.getUsername()));
    try {
      var customerId = stripeService.createCustomer(params).getId();
      account.getBilling().setCustomerId(customerId);
      return billingRepository.save(account.getBilling());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CREATE_CUSTOMER_ERROR);
    }
  }

  /**
   * Delete a stripe customer.
   * 
   * @param stripeCustomerId The unique stripe customer identifier
   * @return The deleted stripe customer id
   */
  public String deleteCustomer(String stripeCustomerId) {
    try {
      return stripeService.deleteCustomer(stripeCustomerId).getId();
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_DELETE_CUSTOMER_ERROR);
    }
  }

  /**
   * Create a new stripe subscription for the given customer id to the given price
   * id.
   * 
   * @param username        The authenticated account username
   * @param paymentMethodId The default payment method id
   * @param plan            The plan id to subscribe to
   * @return The customer billing information
   */
  public Billing createSubscription(String username, String paymentMethodId, String plan) {
    var account = accountService.findAuthenticatedAccount(username);
    if (StringUtils.isBlank(account.getBilling().getCustomerId())) {
      throw new ApiException(BillingCode.STRIPE_CUSTOMER_NOT_FOUND);
    }
    if (!StringUtils.isBlank(account.getBilling().getSubscriptionId())) {
      throw new ApiException(BillingCode.STRIPE_CREATE_SUBSCRIPTION_CONFLICT);
    }
    if (findAllPlans().getData().stream().filter(p -> p.getId().equals(plan)).findFirst().isEmpty()) {
      throw new ApiException(BillingCode.STRIPE_PLAN_NOT_FOUND);
    }
    var billing = account.getBilling();
    try {
      var paymentMethod = createPaymentMethod(billing.getCustomerId(), paymentMethodId);
      updateDefaultPaymentMethod(billing.getCustomerId(), paymentMethod.getId());
      var subscription = createSubscription(billing.getCustomerId(), plan);
      mapToBilling(billing, paymentMethod, subscription);
      return billingRepository.save(account.getBilling());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CREATE_SUBSCRIPTION_ERROR);
    }
  }

  private Subscription createSubscription(String customerId, String plan) throws StripeException {
    Map<String, Object> params = Map.of(KEY_CUSTOMER, customerId, KEY_ITEMS, List.of(Map.of(KEY_PRICE, plan)));
    return stripeService.createSubscription(params);
  }

  private PaymentMethod createPaymentMethod(String customerId, String paymentMethodId) throws StripeException {
    Map<String, Object> params = Map.of(KEY_CUSTOMER, customerId);
    return stripeService.attachPaymentMethod(paymentMethodId, params);
  }

  private Customer updateDefaultPaymentMethod(String customerId, String paymentMethodId) throws StripeException {
    Map<String, Object> params = Map.of(KEY_INVOICE_SETTINGS, Map.of(KEY_DEFAULT_PAYMENT_METHOD, paymentMethodId));
    return stripeService.updateCustomer(customerId, params);
  }

  private void mapToBilling(Billing billing, PaymentMethod paymentMethod, Subscription subscription) {
    var subscriptionItem = subscription.getItems().getData().stream().findFirst()
        .orElseThrow(() -> new ApiException(BillingCode.STRIPE_SUBSCRIPTION_ITEM_NOT_FOUND));
    mapPaymentMethod(billing, paymentMethod);
    billing.setSubscriptionId(subscription.getId());
    billing.setSubscriptionItemId(subscriptionItem.getId());
    billing.setSubscriptionStatus(subscription.getStatus());
    billing.setSubscriptionCurrentPeriodStart(toOffsetDateTime(subscription.getCurrentPeriodStart()));
    billing.setSubscriptionCurrentPeriodEnd(toOffsetDateTime(subscription.getCurrentPeriodEnd()));
    billing.setPlanId(subscription.getPlan().getId());
    billing.setPlanNickname(subscription.getPlan().getNickname());
    billing.setCreatedDate(toOffsetDateTime(subscription.getCreated()));
  }

  private void mapPaymentMethod(Billing billing, PaymentMethod paymentMethod) {
    if (paymentMethod != null) {
      billing.setCardBillingName(paymentMethod.getBillingDetails().getName());
      billing.setCardBrand(paymentMethod.getCard().getBrand());
      billing.setCardLast4(paymentMethod.getCard().getLast4());
      billing.setCardExpMonth(paymentMethod.getCard().getExpMonth().shortValue());
      billing.setCardExpYear(paymentMethod.getCard().getExpYear().shortValue());
    }
  }

  /**
   * Update an existing stripe subscription for the given customer id to the given
   * price id. If a subscription is downgrading from a paid plan to the free plan,
   * this is handled as a cancellation of service. In this case, unused operations
   * are refunded and the customer is billed immediately.
   * 
   * @param username The authenticated account username
   * @param plan     The plan id to subscribe to
   * @return The customer billing information
   */
  public Billing updateSubscription(String username, String plan) {
    var account = accountService.findAuthenticatedAccount(username);
    if (StringUtils.isBlank(account.getBilling().getCustomerId())) {
      throw new ApiException(BillingCode.STRIPE_CUSTOMER_NOT_FOUND);
    }
    if (StringUtils.isBlank(account.getBilling().getSubscriptionId())) {
      throw new ApiException(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND);
    }
    var plans = findAllPlans().getData();
    var newPlan = plans.stream().filter(p -> p.getId().equals(plan)).findFirst();
    if (newPlan.isEmpty()) {
      throw new ApiException(BillingCode.STRIPE_PLAN_NOT_FOUND);
    }
    if (account.getBilling().getSubscriptionStatus().equals(KEY_ACTIVE)
        && account.getBilling().getPlanId().equals(newPlan.get().getId())) {
      throw new ApiException(BillingCode.STRIPE_UPDATE_SUBSCRIPTION_INVALID);
    }
    try {
      var existingSubscription = stripeService.retrieveSubscription(account.getBilling().getSubscriptionId());
      if (isCancelling(existingSubscription.getPlan().getNickname(), newPlan.get().getNickname())) {
        return cancelSubscription(account, existingSubscription,
            plans.stream().filter(p -> p.getNickname().equals(PLAN_STARTER)).findFirst().get().getId());
      } else if (isUpgrading(existingSubscription.getPlan().getNickname(), newPlan.get().getNickname())) {
        return updateSubscription(account, existingSubscription, plan, BILLING_CYCLE_NOW, PRORATION_BEHAVIOUR_NONE);
      }
      return updateSubscription(account, existingSubscription, plan, BILLING_CYCLE_UNCHANGED, PRORATION_BEHAVIOUR_NONE);
    } catch (StripeException e) {
      throw new ApiException(BillingCode.STRIPE_UPDATE_SUBSCRIPTION_ERROR);
    }
  }

  private Billing updateSubscription(Account account, Subscription existingSubscription, String plan,
      String billingCycle, String prorationBehaviour) throws StripeException {
    var subscription = stripeService.updateSubscription(existingSubscription.getId(), Map.of(//
        KEY_CANCEL_AT_PERIOD_END, false, //
        KEY_BILLING_CYCLE_ANCHOR, billingCycle, //
        KEY_PRORATION_BEHAVIOUR, prorationBehaviour, //
        KEY_ITEM_0_ID, existingSubscription.getItems().getData().get(0).getId(), //
        KEY_ITEM_0_PRICE, plan));
    mapToBilling(account.getBilling(), null, subscription);
    account.getBilling().setUpdatedDate(OffsetDateTime.now());
    return billingRepository.save(account.getBilling());
  }

  private boolean isCancelling(String existingPlanNickname, String newPlanNickname) {
    return (existingPlanNickname.equals(PLAN_PRO) && newPlanNickname.equals(PLAN_STARTER))
        || (existingPlanNickname.equals(PLAN_INDIE) && newPlanNickname.equals(PLAN_STARTER));
  }

  private boolean isUpgrading(String existingPlanNickname, String newPlanNickname) {
    return (existingPlanNickname.equals(PLAN_STARTER) && newPlanNickname.equals(PLAN_PRO))
        || (existingPlanNickname.equals(PLAN_STARTER) && newPlanNickname.equals(PLAN_INDIE));
  }

  /**
   * Cancel an active stripe subscription. Only valid on paid plans. Any unused
   * operations are refunded and the customer is billed immediately. There
   * subscription is then switched to a free plan.
   * 
   * @param username The authenticated account username
   * @return The customer billing information
   */
  public Billing cancelSubscription(String username) {
    var account = accountService.findAuthenticatedAccount(username);
    if (StringUtils.isBlank(account.getBilling().getCustomerId())) {
      throw new ApiException(BillingCode.STRIPE_CUSTOMER_NOT_FOUND);
    }
    if (StringUtils.isBlank(account.getBilling().getSubscriptionId())) {
      throw new ApiException(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND);
    }
    try {
      var existingSubscription = stripeService.retrieveSubscription(account.getBilling().getSubscriptionId());
      return cancelSubscription(account, existingSubscription, findAllPlans().getData().stream()
          .filter(p -> p.getNickname().equals(PLAN_STARTER)).findFirst().get().getId());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_ERROR);
    }
  }

  private Billing cancelSubscription(Account account, Subscription existingSubscription, String starterPlan)
      throws StripeException {
    if (existingSubscription.getPlan().getNickname().equals(PLAN_STARTER)) {
      throw new ApiException(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_INVALID);
    }
    refundUnusedOperations(account, existingSubscription);
    return updateSubscription(account, existingSubscription, starterPlan, BILLING_CYCLE_NOW,
        PRORATION_BEHAVIOUR_ALWAYS);
  }

  private void refundUnusedOperations(Account account, Subscription subscription) throws StripeException {
    var flatTier = subscription.getItems().getData().get(0).getPlan().getTiers().get(0);
    var maxOperations = flatTier.getUpTo();
    var upcomingInvoice = stripeService.findUpcomingInvoice(Map.of(KEY_CUSTOMER, account.getBilling().getCustomerId()));
    var currentOperations = upcomingInvoice.getLines().getData().get(0).getQuantity();

    if (maxOperations - currentOperations > 0) {
      stripeService.createInvoiceItem(Map.of(//
          KEY_CUSTOMER, account.getBilling().getCustomerId(), //
          KEY_DESCRIPTION, "Unused operation(s)", //
          KEY_CURRENCY, "gbp", //
          KEY_QUANTITY, maxOperations - currentOperations, //
          KEY_UNIT_AMOUNT_DECIMAL, -((double) flatTier.getFlatAmount() / maxOperations)));
    }
  }

  /**
   * Create a usage record event for an accounts subscription item.
   * 
   * @param username   The authenticated account username
   * @param operations The new number of operations that have occured
   */
  public void createUsageRecord(String username, int operations) {
    var account = accountService.findAuthenticatedAccount(username);
    Map<String, Object> params = Map.of(KEY_QUANTITY, operations, KEY_TIMESTAMP, Instant.now().getEpochSecond());
    try {
      stripeService.createUsageRecord(account.getBilling().getSubscriptionItemId(), params);
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_USAGE_RECORD_ERROR);
    }
  }

  /**
   * Find all usage records for subscription item held by stripe.
   * 
   * @param username The authenticated account username
   * @return A list of all usage records for subscription item
   */
  public UsageRecordSummaryCollection findAllUsageRecords(String username) {
    var account = accountService.findAuthenticatedAccount(username);
    try {
      return stripeService.findAllUsageRecords(account.getBilling().getSubscriptionItemId());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_FIND_ALL_USAGE_RECORDS_ERROR);
    }
  }

  /**
   * Find all invoices for stripe customer.
   * 
   * @param username The authenticated account username
   * @return A list of formatted invoice responses
   */
  public List<InvoiceResponse> findAllInvoices(String username) {
    var account = accountService.findAuthenticatedAccount(username);
    Map<String, Object> params = Map.of(KEY_CUSTOMER, account.getBilling().getCustomerId());
    try {
      return stripeService.findAllInvoices(params).getData().stream().map(this::mapToInvoiceResponse)
          .collect(Collectors.toList());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_FIND_ALL_INVOICES_ERROR);
    }
  }

  /**
   * Find upcoming invoice for stripe customer.
   * 
   * @param username The authenticated account username
   * @return A formatted upcoming invoice response
   */
  public InvoiceResponse findUpcomingInvoice(String username) {
    var account = accountService.findAuthenticatedAccount(username);
    Map<String, Object> params = Map.of(KEY_CUSTOMER, account.getBilling().getCustomerId());
    try {
      return mapToInvoiceResponse(stripeService.findUpcomingInvoice(params));
    } catch (StripeException ex) {
      if (ex.getCode().equals(STRIPE_EXCEPTION_NO_INVOICE)) {
        throw new ApiException(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_NOT_FOUND);
      }
      throw new ApiException(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_ERROR);
    }
  }

  private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
    var response = new InvoiceResponse();
    response.setNumber(invoice.getNumber());
    response.setCurrency(invoice.getCurrency());
    response.setSubtotal(invoice.getSubtotal());
    response.setTotal(invoice.getTotal());
    response.setAmountDue(invoice.getAmountDue());
    response.setPeriodStart(toOffsetDateTime(invoice.getPeriodStart()));
    response.setPeriodEnd(toOffsetDateTime(invoice.getPeriodEnd()));
    response.setItems(//
        invoice.getLines().getData().stream().map(InvoiceLineItemResponse::new).collect(Collectors.toList())//
    );
    return response;
  }

  private OffsetDateTime toOffsetDateTime(long timestamp) {
    return OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), TimeZone.getDefault().toZoneId());
  }
}