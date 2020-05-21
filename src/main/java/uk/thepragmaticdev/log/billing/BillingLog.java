package uk.thepragmaticdev.log.billing;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
import java.time.OffsetDateTime;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.log.Log;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class BillingLog extends Log implements Model {

  @ManyToOne
  @CsvIgnore
  private Account account;

  @CsvBindByName
  private String amount; // generated

  /**
   * Entity to capture billing events.
   * 
   * @param id          The primary key
   * @param account     The authenticated account
   * @param action      The billing action
   * @param amount      The action amount
   * @param createdDate The time of the action
   */
  public BillingLog(Long id, Account account, String action, String amount, OffsetDateTime createdDate) {
    super(id, action, createdDate);
    this.account = account;
    this.amount = amount;
  }
}
