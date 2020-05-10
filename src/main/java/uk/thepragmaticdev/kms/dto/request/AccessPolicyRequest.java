package uk.thepragmaticdev.kms.dto.request;

import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessPolicyRequest {

  @Size(min = 1)
  private String name;

  @Size(min = 1)
  private String range;
}