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

  private String interval;

  private int intervalCount;

  private String nickname;

  private List<BillingPlanTierResponse> tiers;

  private String product;
}