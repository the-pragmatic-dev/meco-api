package uk.thepragmaticdev.log.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLogResponse {

  private String action;

  private OffsetDateTime createdDate;

  private RequestMetadata requestMetadata;
}