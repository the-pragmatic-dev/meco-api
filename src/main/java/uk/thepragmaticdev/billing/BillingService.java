package uk.thepragmaticdev.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.PriceCollection;
import com.stripe.model.UsageRecordSummaryCollection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.BillingCode;

@Service
public class BillingService {

  private final AccountService accountService;

  private final StripeService stripeService;

  /**
   * Service for creating stripe customers and handling subscriptions to non-free
   * prices.
   * 
   * @param accountService The service for retrieving account information
   * @param stripeService  The service containing the stripe api
   */
  public BillingService(//
      @Lazy AccountService accountService, //
      StripeService stripeService) {
    this.accountService = accountService;
    this.stripeService = stripeService;
  }

  /**
   * Find all active prices held by stripe.
   * 
   * @return A list of all active prices held by stripe
   */
  public PriceCollection findAllPrices() {
    Map<String, Object> params = Map.of("active", true);
    try {
      return stripeService.findAllPrices(params);
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_FIND_ALL_PRICES_ERROR);
    }
  }

  /**
   * Create a new stripe customer for an authenticated account.
   * 
   * @param username The authenticated account username
   */
  public void createCustomer(String username) {
    var account = accountService.findAuthenticatedAccount(username);
    if (!StringUtils.isBlank(account.getStripeCustomerId())) {
      throw new ApiException(BillingCode.STRIPE_CREATE_CUSTOMER_CONFLICT);
    }
    var params = Map.of("email", account.getUsername(), //
        "description", "MECO Account", //
        "metadata", Map.of("username", account.getUsername()));
    try {
      var stripeCustomerId = stripeService.createCustomer(params).getId();
      accountService.saveCustomerId(account, stripeCustomerId);
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
   * @param username The authenticated account username
   * @param price    The price id to subscribe to
   */
  public void createSubscription(String username, String price) {
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    if (findAllPrices().getData().stream().filter(p -> p.getId().equals(price)).findFirst().isEmpty()) {
      throw new ApiException(BillingCode.STRIPE_PRICE_NOT_FOUND);
    }
    Map<String, Object> params = Map.of("customer", authenticatedAccount.getStripeCustomerId(), "items",
        List.of(Map.of("price", price)));
    try {
      var subscription = stripeService.createSubscription(params);
      var subscriptionItem = subscription.getItems().getData().stream().findFirst()
          .orElseThrow(() -> new ApiException(BillingCode.STRIPE_SUBSCRIPTION_ITEM_NOT_FOUND));
      accountService.saveSubscription(authenticatedAccount, subscription.getId(), subscriptionItem.getId());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CREATE_SUBSCRIPTION_ERROR);
    }
  }

  /**
   * Cancel an active stripe subscription.
   * 
   * @param username The authenticated account username
   */
  public void cancelSubscription(String username) {
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    if (StringUtils.isBlank(authenticatedAccount.getStripeSubscriptionId())) {
      throw new ApiException(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND);
    }
    try {
      stripeService.cancelSubscription(authenticatedAccount.getStripeSubscriptionId()).getId();
      accountService.cancelSubscription(authenticatedAccount);
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_ERROR);
    }
  }

  /**
   * Create a usage record event for an accounts subscription item.
   * 
   * @param username   The authenticated account username
   * @param operations The new number of operations that have occured
   */
  public void createUsageRecord(String username, int operations) {
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    Map<String, Object> params = Map.of("quantity", operations, "timestamp", Instant.now().getEpochSecond());
    try {
      stripeService.createUsageRecord(authenticatedAccount.getStripeSubscriptionItemId(), params);
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
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    try {
      return stripeService.findAllUsageRecords(authenticatedAccount.getStripeSubscriptionItemId());
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_FIND_ALL_USAGE_RECORDS_ERROR);
    }
  }

  /**
   * Find upcoming invoice for stripe customer.
   * 
   * @param username The authenticated account username
   * @return An invoice coming up
   */
  public Invoice findUpcomingInvoice(String username) {
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    Map<String, Object> params = Map.of("customer", authenticatedAccount.getStripeCustomerId());
    try {
      return stripeService.findUpcomingInvoice(params);
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_ERROR);
    }
  }
}