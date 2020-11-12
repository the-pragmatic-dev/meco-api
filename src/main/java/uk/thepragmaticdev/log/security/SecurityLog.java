package uk.thepragmaticdev.log.security;

import com.opencsv.bean.CsvIgnore;
import com.opencsv.bean.CsvRecurse;
import java.time.OffsetDateTime;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.log.Log;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class SecurityLog extends Log implements Model {

  @ManyToOne
  @CsvIgnore
  @ToString.Exclude
  private Account account;

  @Embedded
  @CsvRecurse
  private RequestMetadata requestMetadata; // generated

  /**
   * Entity to capture security events.
   * 
   * @param id              The primary key
   * @param account         The authenticated account
   * @param action          The security action
   * @param requestMetadata The geolocation and ip of the request
   * @param createdDate     The time of the action
   */
  public SecurityLog(Long id, Account account, String action, RequestMetadata requestMetadata,
      OffsetDateTime createdDate) {
    super(id, action, createdDate);
    this.account = account;
    this.requestMetadata = requestMetadata;
  }
}
