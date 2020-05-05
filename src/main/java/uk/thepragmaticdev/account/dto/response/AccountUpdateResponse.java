package uk.thepragmaticdev.account.dto.response;

import java.time.OffsetDateTime;
import javax.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateResponse {

  @Email(message = "Username is not a valid email.")
  private String username;

  private String fullName;

  private boolean emailSubscriptionEnabled;

  private boolean billingAlertEnabled;

  private OffsetDateTime createdDate;
}