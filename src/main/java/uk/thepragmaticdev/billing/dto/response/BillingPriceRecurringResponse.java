package uk.thepragmaticdev.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingPriceRecurringResponse {

  private String interval;

  private long intervalCount;
}
