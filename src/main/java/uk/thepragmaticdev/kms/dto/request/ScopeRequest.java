package uk.thepragmaticdev.kms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScopeRequest {

  private boolean image;

  private boolean gif;

  private boolean text;

  private boolean video;
}