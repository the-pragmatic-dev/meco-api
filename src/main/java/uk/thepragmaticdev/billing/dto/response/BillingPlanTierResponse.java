package uk.thepragmaticdev.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingPlanTierResponse {

  private int flatAmount;

  private double unitAmountDecimal;

  private int upTo;
}
