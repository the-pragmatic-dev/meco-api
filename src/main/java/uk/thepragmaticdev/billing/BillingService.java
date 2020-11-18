package uk.thepragmaticdev.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Plan;
import com.stripe.model.PlanCollection;
import com.stripe.model.Subscription;
import com.stripe.model.UsageRecordSummaryCollection;
import com.stripe.param.SubscriptionUpdateParams;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.billing.dto.response.InvoiceLineItemResponse;
import uk.thepragmaticdev.billing.dto.response.InvoiceResponse;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.BillingCode;
import uk.thepragmaticdev.log.billing.BillingLogService;

@Service
public class BillingService {

  private final BillingRepository billingRepository;

  private final BillingLogService billingLogService;

  private final AccountService accountService;

  private final StripeService stripeService;

  private final int expireGracePeriod;

  public static final String PLAN_STARTER = "starter";

  private static final String PLAN_INDIE = "indie";

  private static final String PLAN_PRO = "pro";

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
      BillingLogService billingLogService, //
      @Lazy AccountService accountService, //
      StripeService stripeService, //
      @Value("${billing.expire-grace-period}") int expireGracePeriod) {
    this.billingRepository = billingRepository;
    this.billingLogService = billingLogService;
    this.accountService = accountService;
    this.stripeService = stripeService;
    this.expireGracePeriod = expireGracePeriod;
  }

  /**
   * Find all active plans held by stripe.
   * 
   * @return A list of all active plans held by stripe
   */
  public PlanCollection findAllPlans() {
    try {
      return stripeService.findAllPlans(true);
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_FIND_ALL_PLANS_ERROR);
    }
  }

  private Plan findPlanByNickname(String nickname) {
    var plan = findAllPlans().getData().stream().filter(p -> p.getNickname().equals(nickname)).findFirst();
    if (plan.isEmpty()) {
      throw new ApiException(BillingCode.STRIPE_PLAN_NOT_FOUND);
    }
    return plan.get();
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
    try {
      var customerId = stripeService.createCustomer(account.getUsername(), "MECO").getId();
      account.getBilling().setCustomerId(customerId);
      var billing = billingRepository.save(account.getBilling());
      billingLogService.createCustomer(account);
      return billing;
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
      var paymentMethod = createPaymentMethod(account, paymentMethodId);
      var subscription = stripeService.createSubscription(billing.getCustomerId(), plan);
      mapToBilling(billing, paymentMethod, subscription);
      billing.setCreatedDate(toOffsetDateTime(subscription.getCreated()));
      var persistedBilling = billingRepository.save(account.getBilling());
      billingLogService.createSubscription(account, persistedBilling.getPlanNickname());
      return persistedBilling;
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CREATE_SUBSCRIPTION_ERROR);
    }
  }

  private void mapToBilling(Billing billing, PaymentMethod paymentMethod, Subscription subscription) {
    var subscriptionItem = subscription.getItems().getData().stream().findFirst()
        .orElseThrow(() -> new ApiException(BillingCode.STRIPE_SUBSCRIPTION_ITEM_NOT_FOUND));
    mapPaymentMethod(billing, paymentMethod);
    billing.setSubscriptionId(subscription.getId());
    billing.setSubscriptionItemId(subscriptionItem.getId());
    billing.setSubscriptionStatus(subscription.getStatus());
    billing.setSubscriptionCurrentPeriodStart(toOffsetDateTime(subscription.getCurrentPeriodStart()));
    // pad extra days to give failed charges a chance to automatically retry
    billing.setSubscriptionCurrentPeriodEnd(
        toOffsetDateTime(subscription.getCurrentPeriodEnd()).plusDays(expireGracePeriod));
    billing.setPlanId(subscriptionItem.getPlan().getId());
    billing.setPlanNickname(subscriptionItem.getPlan().getNickname());
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
    if (account.getBilling().getSubscriptionStatus().equals("active")
        && account.getBilling().getPlanId().equals(newPlan.get().getId())) {
      throw new ApiException(BillingCode.STRIPE_UPDATE_SUBSCRIPTION_INVALID);
    }
    try {
      var existingSubscription = stripeService.retrieveSubscription(account.getBilling().getSubscriptionId());
      var existingNickname = existingSubscription.getItems().getData().get(0).getPlan().getNickname();
      if (isCancelling(existingNickname, newPlan.get().getNickname())) {
        return cancelSubscription(account, existingSubscription, findPlanByNickname(PLAN_STARTER).getId());
      } else if (isUpgrading(existingNickname, newPlan.get().getNickname())) {
        var persistedBilling = updateSubscription(account, existingSubscription, plan,
            SubscriptionUpdateParams.BillingCycleAnchor.NOW, SubscriptionUpdateParams.ProrationBehavior.NONE);
        billingLogService.updateSubscription(account, PLAN_STARTER, newPlan.get().getNickname());
        return persistedBilling;
      }
      var persistedBilling = updateSubscription(account, existingSubscription, plan,
          SubscriptionUpdateParams.BillingCycleAnchor.UNCHANGED, SubscriptionUpdateParams.ProrationBehavior.NONE);
      billingLogService.updateSubscription(account, existingNickname, newPlan.get().getNickname());
      return persistedBilling;
    } catch (StripeException e) {
      throw new ApiException(BillingCode.STRIPE_UPDATE_SUBSCRIPTION_ERROR);
    }
  }

  private Billing updateSubscription(Account account, Subscription existingSubscription, String plan,
      SubscriptionUpdateParams.BillingCycleAnchor billingCycleAnchor,
      SubscriptionUpdateParams.ProrationBehavior prorationBehaviour) throws StripeException {
    var subscription = stripeService.updateSubscription(existingSubscription.getId(), //
        false, //
        billingCycleAnchor, //
        prorationBehaviour, //
        existingSubscription.getItems().getData().get(0).getId(), //
        plan);
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
      return cancelSubscription(account, existingSubscription, findPlanByNickname(PLAN_STARTER).getId());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_ERROR);
    }
  }

  private Billing cancelSubscription(Account account, Subscription existingSubscription, String starterPlan)
      throws StripeException {
    var existingNickname = existingSubscription.getItems().getData().get(0).getPlan().getNickname();
    if (existingNickname.equals(PLAN_STARTER)) {
      throw new ApiException(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_INVALID);
    }
    refundUnusedOperations(account, existingSubscription);
    var persistedBilling = updateSubscription(account, existingSubscription, starterPlan,
        SubscriptionUpdateParams.BillingCycleAnchor.NOW, SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE);
    billingLogService.updateSubscription(account, existingNickname, PLAN_STARTER);
    return persistedBilling;
  }

  private void refundUnusedOperations(Account account, Subscription subscription) throws StripeException {
    var flatTier = subscription.getItems().getData().get(0).getPlan().getTiers().get(0);
    var maxOperations = flatTier.getUpTo();
    var upcomingInvoice = stripeService.findUpcomingInvoice(account.getBilling().getCustomerId());
    var currentOperations = upcomingInvoice.getLines().getData().get(0).getQuantity();
    var quantity = maxOperations - currentOperations;

    if (quantity > 0) {
      var unitAmountDecimal = -((double) flatTier.getFlatAmount() / maxOperations);
      stripeService.createInvoiceItem(account.getBilling().getCustomerId(), "Unused operation(s)", "gbp", quantity,
          unitAmountDecimal);
      billingLogService.refundUnusedOperations(account, quantity, unitAmountDecimal);
    }
  }

  /**
   * Create a new stripe payment method for the given customer.
   * 
   * @param username        The authenticated account username
   * @param paymentMethodId The default payment method id
   * @return
   */
  public Billing createPaymentMethod(String username, String paymentMethodId) {
    var account = accountService.findAuthenticatedAccount(username);
    var billing = account.getBilling();
    if (StringUtils.isBlank(billing.getCustomerId())) {
      throw new ApiException(BillingCode.STRIPE_CUSTOMER_NOT_FOUND);
    }
    try {
      var paymentMethod = createPaymentMethod(account, paymentMethodId);
      mapPaymentMethod(billing, paymentMethod);
      billing.setUpdatedDate(OffsetDateTime.now());
      return billingRepository.save(account.getBilling());
    } catch (StripeException e) {
      throw new ApiException(BillingCode.STRIPE_CREATE_PAYMENT_METHOD_ERROR);
    }
  }

  private PaymentMethod createPaymentMethod(Account account, String paymentMethodId) throws StripeException {
    var paymentMethod = stripeService.attachPaymentMethod(paymentMethodId, account.getBilling().getCustomerId());
    billingLogService.attachPaymentMethod(account, paymentMethod.getCard().getLast4());
    stripeService.updateCustomer(account.getBilling().getCustomerId(), paymentMethod.getId());
    billingLogService.defaultPaymentMethod(account, paymentMethod.getCard().getLast4());
    return paymentMethod;
  }

  /**
   * Sync subscription information within billing data to new data from stripe
   * webhook.
   * 
   * @param eventType The received stripe event type.
   * @param invoice   The received invoice.
   */
  public void handleInvoicePayment(String eventType, Invoice invoice) {
    try {
      if (invoice.getCustomer() == null) {
        throw new ApiException(BillingCode.STRIPE_CUSTOMER_NOT_FOUND);
      }
      var subscription = stripeService.retrieveSubscription(invoice.getSubscription());
      var billing = billingRepository.findByCustomerId(invoice.getCustomer())
          .orElseThrow(() -> new ApiException(BillingCode.BILLING_NOT_FOUND));
      logEvent(eventType, invoice, billing);
      // period end in the past so ignore as it's an old invoice being paid.
      if (toOffsetDateTime(subscription.getCurrentPeriodEnd()).isBefore(OffsetDateTime.now())) {
        return;
      }
      accountService.unfreeze(billing.getAccount());
      mapToBilling(billing, null, subscription);
      billing.setUpdatedDate(OffsetDateTime.now());
      billingRepository.save(billing);
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_SYNC_SUBSCRIPTION_ERROR);
    }
  }

  private void logEvent(String eventType, Invoice invoice, Billing billing) {
    switch (eventType) {
      case "invoice.payment_succeeded":
        billingLogService.invoicePaymentSucceeded(billing.getAccount(), invoice.getNumber(), invoice.getAmountPaid());
        break;
      case "invoice.payment_failed":
        var failedInvoice = findInvoiceById(invoice.getId());
        billingLogService.invoicePaymentFailed(billing.getAccount(), failedInvoice.getNumber());
        break;
      default:
    }
  }

  /**
   * Handle a delinquent customer after all payment retries have been declined.
   * Remove subscription infomation and set status to cancelled.
   * 
   * @param subscription The automatically cancelled subscription
   */
  public void handleDelinquentCustomer(Subscription subscription) {
    if (subscription.getCustomer() == null) {
      throw new ApiException(BillingCode.STRIPE_CUSTOMER_NOT_FOUND);
    }
    var billing = billingRepository.findByCustomerId(subscription.getCustomer())
        .orElseThrow(() -> new ApiException(BillingCode.BILLING_NOT_FOUND));
    billingLogService.delinquentCustomer(billing.getAccount(), billing.getPlanNickname());
    billing.setSubscriptionId(null);
    billing.setSubscriptionItemId(null);
    billing.setSubscriptionStatus(subscription.getStatus());
    billing.setSubscriptionCurrentPeriodStart(null);
    billing.setSubscriptionCurrentPeriodEnd(null);
    billing.setPlanId(null);
    billing.setPlanNickname(null);
    billing.setUpdatedDate(OffsetDateTime.now());
    billingRepository.save(billing);
  }

  /**
   * Create a usage record event for an accounts subscription item.
   * 
   * @param username   The authenticated account username
   * @param operations The new number of operations that have occured
   */
  public void createUsageRecord(String username, int operations) {
    var account = accountService.findAuthenticatedAccount(username);
    try {
      stripeService.createUsageRecord(account.getBilling().getSubscriptionItemId(), operations,
          Instant.now().getEpochSecond());
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
    try {
      return stripeService.findAllInvoices(account.getBilling().getCustomerId()).getData().stream()
          .map(this::mapToInvoiceResponse).collect(Collectors.toList());
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
    try {
      return mapToInvoiceResponse(stripeService.findUpcomingInvoice(account.getBilling().getCustomerId()));
    } catch (StripeException ex) {
      if (ex.getCode().equals(STRIPE_EXCEPTION_NO_INVOICE)) {
        throw new ApiException(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_NOT_FOUND);
      }
      throw new ApiException(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_ERROR);
    }
  }

  /**
   * Find invoice by id.
   * 
   * @param invoiceId The identifier of invoice you'd like to retrieve.
   * @return An invoice
   */
  public InvoiceResponse findInvoiceById(String invoiceId) {
    try {
      return mapToInvoiceResponse(stripeService.findInvoiceById(invoiceId));
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_FIND_INVOICE_NOT_FOUND);
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