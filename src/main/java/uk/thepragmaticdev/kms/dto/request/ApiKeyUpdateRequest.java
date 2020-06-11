package uk.thepragmaticdev.kms.dto.request;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.kms.validation.Ipv4Cidr;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyUpdateRequest {

  @Size(min = 1, max = 50, message = "API key name length must be between 1-50.")
  private String name;

  private Boolean enabled;

  private ScopeRequest scope;

  @Valid
  @Ipv4Cidr
  private List<AccessPolicyRequest> accessPolicies;
}