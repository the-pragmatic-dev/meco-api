package uk.thepragmaticdev.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.log.billing.BillingLog;
import uk.thepragmaticdev.log.security.SecurityLog;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class Account implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  private Long id;

  @Column(unique = true, nullable = false)
  private String stripeCustomerId;

  private String stripeSubscriptionId;

  private String stripeSubscriptionItemId;

  @Column(unique = true, nullable = false)
  @Email(message = "Username is not a valid email.")
  private String username;

  @Size(min = 8, message = "Minimum password length: 8 characters.")
  @JsonProperty(access = Access.WRITE_ONLY)
  @Column(columnDefinition = "bpchar")
  private String password;

  @Column(columnDefinition = "bpchar")
  @JsonIgnore
  private String passwordResetToken;

  @JsonIgnore
  private OffsetDateTime passwordResetTokenExpire;

  private String fullName;

  @Column(columnDefinition = "boolean not null default false")
  private boolean emailSubscriptionEnabled;

  @Column(columnDefinition = "boolean not null default false")
  private boolean billingAlertEnabled;

  private OffsetDateTime createdDate; // generated

  @ElementCollection(fetch = FetchType.EAGER)
  @Column(name = "name")
  @Enumerated(EnumType.STRING)
  @JsonIgnore
  private List<Role> roles;

  @Valid
  @OneToMany(mappedBy = "account", cascade = { CascadeType.ALL }, orphanRemoval = true)
  @JsonIgnore
  private List<ApiKey> apiKeys;

  @OneToMany(mappedBy = "account", cascade = { CascadeType.ALL }, orphanRemoval = true)
  @JsonIgnore
  private List<BillingLog> billingLogs;

  @OneToMany(mappedBy = "account", cascade = { CascadeType.ALL }, orphanRemoval = true)
  @JsonIgnore
  private List<SecurityLog> securityLogs;
}
