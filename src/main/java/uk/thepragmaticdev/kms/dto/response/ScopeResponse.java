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

  private TextScopeResponse text;

  private boolean video;
}