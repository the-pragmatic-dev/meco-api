package uk.thepragmaticdev.billing.dto.request;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingCreatePaymentMethodRequest {

  @NotBlank(message = "payment method id cannot be blank.")
  private String paymentMethodId;
}