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
    Map<String, Object> params = Map.of("active", true);
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
    var params = Map.of("email", account.getUsername(), //
        "description", "MECO Account", //
        "metadata", Map.of("username", account.getUsername()));
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
    Map<String, Object> params = Map.of("customer", customerId, "items", List.of(Map.of("price", plan)));
    return stripeService.createSubscription(params);
  }

  private PaymentMethod createPaymentMethod(String customerId, String paymentMethodId) throws StripeException {
    Map<String, Object> params = Map.of("customer", customerId);
    return stripeService.attachPaymentMethod(paymentMethodId, params);
  }

  private Customer updateDefaultPaymentMethod(String customerId, String paymentMethodId) throws StripeException {
    Map<String, Object> params = Map.of("invoice_settings", Map.of("default_payment_method", paymentMethodId));
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
    if (account.getBilling().getSubscriptionStatus().equals("active")
        && account.getBilling().getPlanId().equals(newPlan.get().getId())) {
      throw new ApiException(BillingCode.STRIPE_UPDATE_SUBSCRIPTION_INVALID);
    }
    try {
      var existingSubscription = stripeService.retrieveSubscription(account.getBilling().getSubscriptionId());
      if (isCancelling(existingSubscription.getPlan().getNickname(), newPlan.get().getNickname())) {
        return cancelSubscription(account, existingSubscription,
            plans.stream().filter(p -> p.getNickname().equals("starter")).findFirst().get().getId());
      } else if (isUpgrading(existingSubscription.getPlan().getNickname(), newPlan.get().getNickname())) {
        return updateSubscription(account, existingSubscription, plan, "now", "none");
      }
      return updateSubscription(account, existingSubscription, plan, "unchanged", "none");
    } catch (StripeException e) {
      throw new ApiException(BillingCode.STRIPE_UPDATE_SUBSCRIPTION_ERROR);
    }
  }

  private Billing updateSubscription(Account account, Subscription existingSubscription, String plan,
      String billingCycle, String prorationBehaviour) throws StripeException {
    var subscription = stripeService.updateSubscription(existingSubscription.getId(), Map.of(//
        "cancel_at_period_end", false, //
        "billing_cycle_anchor", billingCycle, //
        "proration_behavior", prorationBehaviour, //
        "items[0][id]", existingSubscription.getItems().getData().get(0).getId(), //
        "items[0][price]", plan));
    mapToBilling(account.getBilling(), null, subscription);
    account.getBilling().setUpdatedDate(OffsetDateTime.now());
    return billingRepository.save(account.getBilling());
  }

  private boolean isCancelling(String existingPlanNickname, String newPlanNickname) {
    return (existingPlanNickname.equals("pro") && newPlanNickname.equals("starter"))
        || (existingPlanNickname.equals("indie") && newPlanNickname.equals("starter"));
  }

  private boolean isUpgrading(String existingPlanNickname, String newPlanNickname) {
    return (existingPlanNickname.equals("starter") && newPlanNickname.equals("pro"))
        || (existingPlanNickname.equals("starter") && newPlanNickname.equals("indie"));
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
      return cancelSubscription(account, existingSubscription,
          findAllPlans().getData().stream().filter(p -> p.getNickname().equals("starter")).findFirst().get().getId());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_ERROR);
    }
  }

  private Billing cancelSubscription(Account account, Subscription existingSubscription, String starterPlan)
      throws StripeException {
    if (existingSubscription.getPlan().getNickname().equals("starter")) {
      throw new ApiException(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_INVALID);
    }
    refundUnusedOperations(account, existingSubscription);
    return updateSubscription(account, existingSubscription, starterPlan, "now", "always_invoice");
  }

  private void refundUnusedOperations(Account account, Subscription subscription) throws StripeException {
    var flatTier = subscription.getItems().getData().get(0).getPlan().getTiers().get(0);
    var maxOperations = flatTier.getUpTo();
    var upcomingInvoice = stripeService.findUpcomingInvoice(Map.of("customer", account.getBilling().getCustomerId()));
    var currentOperations = upcomingInvoice.getLines().getData().get(0).getQuantity();

    if (maxOperations - currentOperations > 0) {
      stripeService.createInvoiceItem(Map.of(//
          "customer", account.getBilling().getCustomerId(), //
          "description", "Unused operation(s)", //
          "currency", "gbp", //
          "quantity", maxOperations - currentOperations, //
          "unit_amount_decimal", -((double) flatTier.getFlatAmount() / maxOperations)));
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
    Map<String, Object> params = Map.of("quantity", operations, "timestamp", Instant.now().getEpochSecond());
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
    Map<String, Object> params = Map.of("customer", account.getBilling().getCustomerId());
    try {
      return stripeService.findAllInvoices(params).getData().stream().map(invoice -> mapToInvoiceResponse(invoice))
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
    Map<String, Object> params = Map.of("customer", account.getBilling().getCustomerId());
    try {
      return mapToInvoiceResponse(stripeService.findUpcomingInvoice(params));
    } catch (StripeException ex) {
      if (ex.getCode().equals("invoice_upcoming_none")) {
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