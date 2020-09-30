package uk.thepragmaticdev.account.dto.request;

import javax.validation.constraints.Size;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateRequest {

  @Size(min = 1, max = 50, message = "Full name length must be between 1-50.")
  private String fullName;

  private Boolean emailSubscriptionEnabled;

  private Boolean billingAlertEnabled;

  @Min(value = 0)
  @Max(value = 30000)
  private short billingAlertAmount;
}