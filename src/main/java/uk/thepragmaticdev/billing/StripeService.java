package uk.thepragmaticdev.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Plan;
import com.stripe.model.PlanCollection;
import com.stripe.model.Subscription;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

  public void setApiKey(String apiKey) {
    Stripe.apiKey = apiKey;
  }

  public PlanCollection findAllPlans(Map<String, Object> params) throws StripeException {
    return Plan.list(params);
  }

  public Customer createCustomer(Map<String, Object> params) throws StripeException {
    return Customer.create(params);
  }

  public Subscription createSubscription(Map<String, Object> params) throws StripeException {
    return Subscription.create(params);
  }

  public Subscription cancelSubscription(String subscriptionId) throws StripeException {
    return Subscription.retrieve(subscriptionId).cancel();
  }
}