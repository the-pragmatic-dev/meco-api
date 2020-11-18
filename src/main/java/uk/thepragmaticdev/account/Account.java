package uk.thepragmaticdev.account;

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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.billing.Billing;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.log.billing.BillingLog;
import uk.thepragmaticdev.log.security.SecurityLog;
import uk.thepragmaticdev.security.token.RefreshToken;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class Account implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private short avatar;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(columnDefinition = "bpchar")
  private String password;

  @Column(columnDefinition = "bpchar")
  private String passwordResetToken;

  private OffsetDateTime passwordResetTokenExpire;

  private String fullName;

  @Column(columnDefinition = "boolean not null default false")
  private Boolean emailSubscriptionEnabled;

  @Column(columnDefinition = "boolean not null default false")
  private Boolean billingAlertEnabled;

  private short billingAlertAmount;

  private Boolean frozen;

  private OffsetDateTime createdDate; // generated

  @OneToOne(cascade = { CascadeType.ALL }, orphanRemoval = true)
  private Billing billing;

  @ElementCollection(fetch = FetchType.EAGER)
  @Column(name = "name")
  @Enumerated(EnumType.STRING)
  private List<Role> roles;

  @OneToMany(mappedBy = "account", cascade = { CascadeType.ALL }, orphanRemoval = true)
  private List<ApiKey> apiKeys;

  @OneToMany(mappedBy = "account", cascade = { CascadeType.ALL }, orphanRemoval = true)
  private List<BillingLog> billingLogs;

  @OneToMany(mappedBy = "account", cascade = { CascadeType.ALL }, orphanRemoval = true)
  private List<SecurityLog> securityLogs;

  @OneToMany(mappedBy = "account", cascade = { CascadeType.ALL }, orphanRemoval = true)
  private List<RefreshToken> refreshTokens;
}
