package uk.thepragmaticdev.security.token;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair {

  private String accessToken;

  private UUID refreshToken;
}