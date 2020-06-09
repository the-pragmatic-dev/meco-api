package uk.thepragmaticdev.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthSignupRequest {

  @Email(message = "Username is not a valid email.")
  private String username;

  @Size(min = 8, message = "Minimum password length: 8 characters.")
  @ToString.Exclude
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;
}