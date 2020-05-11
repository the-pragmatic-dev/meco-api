package uk.thepragmaticdev.kms.dto.request;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.kms.validation.Ipv4Cidr;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyUpdateRequest {

  @Size(min = 3, max = 20)
  private String name;

  private boolean enabled;

  @NotNull
  private ScopeRequest scope;

  @Ipv4Cidr
  private List<AccessPolicyRequest> accessPolicies;
}