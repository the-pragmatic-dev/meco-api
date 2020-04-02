package uk.thepragmaticdev.billing;

import com.stripe.Stripe;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

  // TODO INSECURE! Static key should be kept on a config-server.
  private final String stripeSecretKey;

  public BillingService(@Value("${stripe.secret-key}") String stripeSecretKey) {
    this.stripeSecretKey = stripeSecretKey;
  }

  /**
   * TODO.
   * 
   * @param email TODO
   * @param token TODO
   * @return
   */
  public String createCustomer(String email, String token) {
    String id = null;
    try {
      Stripe.apiKey = stripeSecretKey;
      Map<String, Object> customerParams = new HashMap<>();
      // add customer unique id here to track them in your web application
      customerParams.put("description", "Customer for " + email);
      customerParams.put("email", email);

      customerParams.put("source", token); // ^ obtained with Stripe.js
      // create a new customer
      Customer customer = Customer.create(customerParams);
      id = customer.getId();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return id;
  }

  /**
   * TODO.
   * 
   * @param customerId TODO
   * @param plan       TODO
   * @param coupon     TODO
   * @return
   */
  public String createSubscription(String customerId, String plan, String coupon) {
    String id = null;
    try {
      Stripe.apiKey = stripeSecretKey;
      Map<String, Object> item = new HashMap<>();
      item.put("plan", plan);

      Map<String, Object> items = new HashMap<>();
      items.put("0", item);

      Map<String, Object> params = new HashMap<>();
      params.put("customer", customerId);
      params.put("items", items);

      // add coupon if available
      if (!coupon.isEmpty()) {
        params.put("coupon", coupon);
      }

      Subscription sub = Subscription.create(params);
      id = sub.getId();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return id;
  }

  /**
   * TODO.
   * 
   * @param subscriptionId TODO
   * @return
   */
  public boolean cancelSubscription(String subscriptionId) {
    boolean status;
    try {
      Stripe.apiKey = stripeSecretKey;
      Subscription sub = Subscription.retrieve(subscriptionId);
      sub.cancel();
      status = true;
    } catch (Exception ex) {
      ex.printStackTrace();
      status = false;
    }
    return status;
  }

  /**
   * TODO.
   * 
   * @param code TODO
   * @return
   */
  public Coupon retrieveCoupon(String code) {
    try {
      Stripe.apiKey = stripeSecretKey;
      return Coupon.retrieve(code);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}