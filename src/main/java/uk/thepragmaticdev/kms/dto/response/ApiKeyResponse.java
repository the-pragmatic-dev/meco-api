package uk.thepragmaticdev.kms.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

  private Long id;

  private String name;

  private String prefix;

  private OffsetDateTime createdDate;

  private OffsetDateTime lastUsedDate;

  private OffsetDateTime modifiedDate;

  private boolean frozen;

  private boolean enabled;

  private ScopeResponse scope;

  private List<AccessPolicyResponse> accessPolicies;
}