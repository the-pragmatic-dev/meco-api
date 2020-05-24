package uk.thepragmaticdev.auth.dto.request;

import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResetRequest {

  @Size(min = 8, message = "Minimum password length: 8 characters.")
  private String password;
}