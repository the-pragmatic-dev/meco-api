package uk.thepragmaticdev.kms;

import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.kms.scope.Scope;
import uk.thepragmaticdev.kms.usage.ApiKeyUsage;
import uk.thepragmaticdev.log.key.ApiKeyLog;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class ApiKey implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String prefix; // generated

  @Transient
  private String key; // generated

  private String hash; // generated

  private OffsetDateTime createdDate;

  private OffsetDateTime lastUsedDate;

  private OffsetDateTime modifiedDate;

  private OffsetDateTime deletedDate;

  private Boolean frozen;

  private Boolean enabled;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private Scope scope;

  @OneToMany(mappedBy = "apiKey", cascade = { CascadeType.ALL }, orphanRemoval = true)
  @ToString.Exclude
  private List<AccessPolicy> accessPolicies;

  @OneToMany(mappedBy = "apiKey", cascade = { CascadeType.ALL }, orphanRemoval = true)
  @ToString.Exclude
  private List<ApiKeyLog> apiKeyLogs;

  @OneToMany(mappedBy = "apiKey", cascade = { CascadeType.ALL }, orphanRemoval = true)
  @ToString.Exclude
  private List<ApiKeyUsage> apiKeyUsages;

  @ManyToOne
  @ToString.Exclude
  private Account account;
}
