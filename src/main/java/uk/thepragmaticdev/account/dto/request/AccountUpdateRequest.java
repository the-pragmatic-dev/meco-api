package uk.thepragmaticdev.account.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateRequest {

  private String fullName;

  private boolean emailSubscriptionEnabled;

  private boolean billingAlertEnabled;
}