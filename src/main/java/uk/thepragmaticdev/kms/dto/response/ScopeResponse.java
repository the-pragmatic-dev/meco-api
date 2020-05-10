package uk.thepragmaticdev.kms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScopeResponse {

  private boolean image;

  private boolean gif;

  private boolean text;

  private boolean video;
}