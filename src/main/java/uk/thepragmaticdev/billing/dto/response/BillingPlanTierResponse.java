package uk.thepragmaticdev.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingPlanTierResponse {

  private long flatAmount;

  private float unitAmountDecimal;

  private long upTo;
}
