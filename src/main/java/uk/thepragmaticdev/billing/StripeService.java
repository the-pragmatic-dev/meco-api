package uk.thepragmaticdev.billing;

import com.stripe.Stripe;
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
import org.springframework.stereotype.Service;

@Service
public class StripeService {

  public void setApiKey(String apiKey) {
    Stripe.apiKey = apiKey;
  }

  public PriceCollection findAllPrices(Map<String, Object> params) throws StripeException {
    return Price.list(params);
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

  public UsageRecord createUsageRecord(String subscriptionItemId, Map<String, Object> params) throws StripeException {
    return UsageRecord.createOnSubscriptionItem(subscriptionItemId, params, RequestOptions.getDefault());
  }

  public UsageRecordSummaryCollection findAllUsageRecords(String subscriptionItemId) throws StripeException {
    return SubscriptionItem.retrieve(subscriptionItemId).usageRecordSummaries();
  }

  public Invoice findUpcomingInvoice(Map<String, Object> params) throws StripeException {
    return Invoice.upcoming(params);
  }
}