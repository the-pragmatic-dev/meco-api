package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum BillingCode implements ErrorCode {

  STRIPE_FIND_ALL_PLANS_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to find all plans with Stripe."),
  STRIPE_CREATE_CUSTOMER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to create new customer with Stripe."),
  STRIPE_CREATE_SUBSCRIPTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to create subscription with Stripe."),
  STRIPE_CANCEL_SUBSCRIPTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to cancel subscription with Stripe."),
  STRIPE_PLAN_NOT_FOUND(HttpStatus.SERVICE_UNAVAILABLE, "Stripe plan not found."),
  STRIPE_SUBSCRIPTION_NOT_FOUND(HttpStatus.SERVICE_UNAVAILABLE, "Stripe subscription not found.");

  private final String message;
  private final HttpStatus status;

  private BillingCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  @Override
  public HttpStatus getStatus() {
    return status;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
