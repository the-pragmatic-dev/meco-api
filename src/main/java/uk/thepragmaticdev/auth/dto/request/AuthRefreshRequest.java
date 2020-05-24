package uk.thepragmaticdev.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.auth.validation.ExpiredAccessToken;
import uk.thepragmaticdev.auth.validation.RefreshToken;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRefreshRequest {

  @ExpiredAccessToken
  private String accessToken;

  @RefreshToken
  private String refreshToken;
}