package uk.thepragmaticdev.billing.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingPlanResponse {

  private String id;

  private String currency;

  private String nickname;

  private String product;

  private String interval;

  private long intervalCount;

  private List<BillingPlanTierResponse> tiers;
}