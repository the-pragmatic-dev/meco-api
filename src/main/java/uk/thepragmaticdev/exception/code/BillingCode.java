package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum BillingCode implements ErrorCode {

  STRIPE_FIND_ALL_PRICES_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to find all prices with Stripe."),
  STRIPE_CREATE_CUSTOMER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to create new customer with Stripe."),
  STRIPE_CREATE_SUBSCRIPTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to create subscription with Stripe."),
  STRIPE_SUBSCRIPTION_ITEM_NOT_FOUND(HttpStatus.SERVICE_UNAVAILABLE, "Stripe subscription item not found."),
  STRIPE_CANCEL_SUBSCRIPTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to cancel subscription with Stripe."),
  STRIPE_USAGE_RECORD_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to update usage record with Stripe."),
  STRIPE_FIND_ALL_USAGE_RECORDS_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to find all usage records with Stripe."),
  STRIPE_FIND_UPCOMING_INVOICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Unable to find upcoming invoice with Stripe."),
  STRIPE_PRICE_NOT_FOUND(HttpStatus.SERVICE_UNAVAILABLE, "Stripe price not found."),
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
