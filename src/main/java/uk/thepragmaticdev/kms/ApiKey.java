package uk.thepragmaticdev.kms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.kms.validation.Ipv4Cidr;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class ApiKey implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Size(min = 3, max = 20)
  private String name;

  private String prefix; // generated

  @Transient
  @JsonIgnore
  private String key; // generated

  @JsonIgnore
  private String hash; // generated

  private OffsetDateTime createdDate;

  private OffsetDateTime lastUsedDate;

  private OffsetDateTime modifiedDate;

  @NotNull
  @Column(columnDefinition = "boolean not null")
  private boolean enabled;

  @NotNull
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnoreProperties({ "apiKey" })
  @JoinColumn(name = "scope_id", referencedColumnName = "id")
  private Scope scope;

  @Ipv4Cidr
  @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true)
  @JsonIgnoreProperties({ "apiKey" })
  @JoinColumn(name = "api_key_id", referencedColumnName = "id")
  private List<AccessPolicy> accessPolicies;

  @ManyToOne
  @JsonIgnore
  private Account account;
}
