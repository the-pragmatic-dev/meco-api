package uk.thepragmaticdev.billing;

import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.endpoint.Model;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class Billing implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = true)
  private String customerId;

  private String subscriptionId;

  private String subscriptionItemId;

  private String subscriptionStatus;

  private OffsetDateTime subscriptionCurrentPeriodStart;

  private OffsetDateTime subscriptionCurrentPeriodEnd;

  private String planId;

  private String planNickname;

  private String cardBillingName;

  private String cardBrand;

  private String cardLast4;

  private Short cardExpMonth;

  private Short cardExpYear;

  private OffsetDateTime createdDate;

  private OffsetDateTime updatedDate;

  @OneToOne(mappedBy = "billing")
  @ToString.Exclude
  private Account account;
}
