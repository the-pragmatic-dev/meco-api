package uk.thepragmaticdev.log.security;

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
public class SecurityLog extends Log implements Model {

  @JsonIgnore
  @CsvIgnore
  private Long accountId;

  private String address; // generated

  /**
   * Entity to capture security events.
   * 
   * @param id        The primary key
   * @param accountId The account id
   * @param action    The billing action
   * @param address   The ip address
   * @param instant   The time of the action
   */
  public SecurityLog(Long id, Long accountId, String action, String address, OffsetDateTime instant) {
    super(id, action, instant);
    this.accountId = accountId;
    this.address = address;
  }
}
