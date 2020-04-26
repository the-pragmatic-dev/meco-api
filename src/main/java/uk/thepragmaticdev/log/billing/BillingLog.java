package uk.thepragmaticdev.log.billing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.CsvIgnore;
import java.time.OffsetDateTime;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.log.Log;

@Data
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class BillingLog extends Log implements Model {

  @JsonIgnore
  @CsvIgnore
  private Long accountId;

  private String amount; // generated

  /**
   * Entity to capture billing events.
   * 
   * @param id        The primary key
   * @param accountId The account id
   * @param action    The billing action
   * @param amount    The action amount
   * @param instant   The time of the action
   */
  public BillingLog(Long id, Long accountId, String action, String amount, OffsetDateTime instant) {
    super(id, action, instant);
    this.accountId = accountId;
    this.amount = amount;
  }
}
