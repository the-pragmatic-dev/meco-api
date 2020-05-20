package uk.thepragmaticdev.billing.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingPriceTierResponse {

  private long flatAmount;

  private BigDecimal unitAmountDecimal;

  private long upTo;
}
