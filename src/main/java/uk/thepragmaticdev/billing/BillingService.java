package uk.thepragmaticdev.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.PlanCollection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
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
   * plans.
   * 
   * @param accountService  The service for retrieving account information
   * @param stripeService   The service containing the stripe api
   * @param stripeSecretKey The secret key for communicating with stripe
   */
  public BillingService(//
      @Lazy AccountService accountService, //
      StripeService stripeService, //
      @Value("${stripe.secret-key}") String stripeSecretKey) {
    this.accountService = accountService;
    this.stripeService = stripeService;
    stripeService.setApiKey(stripeSecretKey);
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
   * Create a new stripe customer.
   * 
   * @param username The email address of the account
   * @return The stripe customer id
   */
  public String createCustomer(String username) {
    Map<String, Object> params = Map.of("email", username, "description", "MECO Account", "metadata",
        Map.of("username", username));
    try {
      return stripeService.createCustomer(params).getId();
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CREATE_CUSTOMER_ERROR);
    }
  }

  /**
   * Create a new stripe subscription for the given customer id to the given plan
   * id.
   * 
   * @param username The authenticated account username
   * @param plan     The plan id to subscribe to
   */
  public void createSubscription(String username, String plan) {
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    if (findAllPlans().getData().stream().filter(p -> p.getId().equals(plan)).findFirst().isPresent()) {
      throw new ApiException(BillingCode.STRIPE_PLAN_NOT_FOUND);
    }
    Map<String, Object> params = Map.of("customer", authenticatedAccount.getStripeCustomerId(), "items",
        List.of(Map.of("plan", plan)));
    try {
      var stripeSubscriptionId = stripeService.createSubscription(params).getId();
      accountService.saveSubscription(authenticatedAccount, stripeSubscriptionId);
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
    } catch (StripeException ex) {
      throw new ApiException(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_ERROR);
    }
  }
}