package uk.thepragmaticdev.auth.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthSignupResponse {

  private String accessToken;

  private UUID refreshToken;
}