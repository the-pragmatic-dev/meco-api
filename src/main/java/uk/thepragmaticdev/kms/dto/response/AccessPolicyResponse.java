package uk.thepragmaticdev.kms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessPolicyResponse {

  private String name;

  private String range;
}