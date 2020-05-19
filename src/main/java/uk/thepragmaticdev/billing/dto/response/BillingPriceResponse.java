package uk.thepragmaticdev.billing.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingPriceResponse {

  private String id;

  private String currency;

  private String nickname;

  private String product;

  private BillingPriceRecurringResponse recurring;

  private List<BillingPriceTierResponse> tiers;
}