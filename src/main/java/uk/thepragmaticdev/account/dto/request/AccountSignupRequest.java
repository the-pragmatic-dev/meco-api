package uk.thepragmaticdev.account.dto.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSignupRequest {

  @Email(message = "Username is not a valid email.")
  private String username;

  @Size(min = 8, message = "Minimum password length: 8 characters.")
  private String password;
}