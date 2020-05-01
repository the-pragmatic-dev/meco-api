package uk.thepragmaticdev.endpoint.controller;

import com.stripe.model.Coupon;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.billing.BillingService;
import uk.thepragmaticdev.endpoint.Response;

@RestController
@RequestMapping("/billing")
@CrossOrigin("*")
@Tag(name = "billing")
public class BillingController {

  private final BillingService billingService;

  @Autowired
  public BillingController(BillingService billingService) {
    this.billingService = billingService;
  }

  /**
   * TODO.
   * 
   * @param email  TODO
   * @param token  TODO
   * @param plan   TODO
   * @param coupon TODO
   * @return
   */
  @PostMapping("/create-subscription")
  public @ResponseBody Response createSubscription(String email, String token, String plan, String coupon) {
    // validate data
    if (token == null || plan.isEmpty()) {
      return new Response(false, "Stripe payment token is missing. Please, try again later.");
    }

    // create customer first
    String customerId = billingService.createCustomer(email, token);

    if (customerId == null) {
      return new Response(false, "An error occurred while trying to create a customer.");
    }

    // create subscription
    String subscriptionId = billingService.createSubscription(customerId, plan, coupon);
    if (subscriptionId == null) {
      return new Response(false, "An error occurred while trying to create a subscription.");
    }

    // Ideally you should store customerId and subscriptionId along with customer
    // object here.
    // These values are required to update or cancel the subscription at later
    // stage.

    return new Response(true, "Success! Your subscription id is " + subscriptionId);
  }

  /**
   * TODO.
   * 
   * @param subscriptionId TODO
   * @return
   */
  @PostMapping("/cancel-subscription")
  public @ResponseBody Response cancelSubscription(String subscriptionId) {
    boolean status = billingService.cancelSubscription(subscriptionId);
    if (!status) {
      return new Response(false, "Failed to cancel the subscription. Please, try later.");
    }
    return new Response(true, "Subscription cancelled successfully.");
  }

  /**
   * TODO.
   * 
   * @param code TODO
   * @return
   */
  @PostMapping("/coupon-validator")
  public @ResponseBody Response couponValidator(String code) {
    Coupon coupon = billingService.retrieveCoupon(code);
    if (coupon != null && coupon.getValid()) {
      String details = (coupon.getPercentOff() == null ? "$" + (coupon.getAmountOff() / 100)
          : coupon.getPercentOff() + "%") + " OFF " + coupon.getDuration();
      return new Response(true, details);
    } else {
      return new Response(false, "This coupon code is not available. This may be because it has expired or has "
          + "already been applied to your account.");
    }
  }
}