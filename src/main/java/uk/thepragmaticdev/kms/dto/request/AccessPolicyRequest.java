package uk.thepragmaticdev.kms.dto.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessPolicyRequest {

  @NotNull(message = "Access policy name cannot be null.")
  @Size(min = 1, max = 50, message = "Access policy name length must be between 1-50.")
  private String name;

  private String range;
}