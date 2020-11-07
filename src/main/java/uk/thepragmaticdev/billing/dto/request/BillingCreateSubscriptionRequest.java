package uk.thepragmaticdev.billing.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingCreateSubscriptionRequest {

  private String paymentMethodId;

  private String planId;
}