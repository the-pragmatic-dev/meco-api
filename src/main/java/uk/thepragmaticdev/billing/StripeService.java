package uk.thepragmaticdev.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.UsageRecord;
import com.stripe.model.UsageRecordSummaryCollection;
import com.stripe.net.RequestOptions;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

  private RequestOptions requestOptions;

  /**
   * Service which wraps the stripe api static library and initialises secret key.
   * 
   * @param stripeSecretKey The secret key for communicating with stripe
   */
  public StripeService(@Value("${stripe.secret-key}") String stripeSecretKey) {
    requestOptions = RequestOptions.builder().setApiKey(stripeSecretKey).build();
  }

  public PriceCollection findAllPrices(Map<String, Object> params) throws StripeException {
    return Price.list(params, requestOptions);
  }

  public Customer createCustomer(Map<String, Object> params) throws StripeException {
    return Customer.create(params, requestOptions);
  }

  public Customer deleteCustomer(String customerId) throws StripeException {
    return Customer.retrieve(customerId, requestOptions).delete(requestOptions);
  }

  public Subscription createSubscription(Map<String, Object> params) throws StripeException {
    return Subscription.create(params, requestOptions);
  }

  public Subscription cancelSubscription(String subscriptionId) throws StripeException {
    return Subscription.retrieve(subscriptionId, requestOptions).cancel(Map.of(), requestOptions);
  }

  public UsageRecord createUsageRecord(String subscriptionItemId, Map<String, Object> params) throws StripeException {
    return UsageRecord.createOnSubscriptionItem(subscriptionItemId, params, requestOptions);
  }

  public UsageRecordSummaryCollection findAllUsageRecords(String subscriptionItemId) throws StripeException {
    return SubscriptionItem.retrieve(subscriptionItemId, requestOptions).usageRecordSummaries(Map.of(), requestOptions);
  }

  public Invoice findUpcomingInvoice(Map<String, Object> params) throws StripeException {
    return Invoice.upcoming(params, requestOptions);
  }
}