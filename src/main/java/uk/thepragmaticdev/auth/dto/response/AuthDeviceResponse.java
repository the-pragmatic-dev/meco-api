package uk.thepragmaticdev.auth.dto.response;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthDeviceResponse {

  private OffsetDateTime createdDate;

  private OffsetDateTime expirationTime;

  private RequestMetadata requestMetadata;
}