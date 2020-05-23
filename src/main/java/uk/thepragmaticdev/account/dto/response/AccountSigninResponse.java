package uk.thepragmaticdev.account.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSigninResponse {

  private String accessToken;

  private UUID refreshToken;
}