package uk.thepragmaticdev.log.key;

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
public class ApiKeyLog extends Log implements Model {

  @JsonIgnore
  @CsvIgnore
  private Long apiKeyId;

  private String address; // generated

  /**
   * Entity to capture api key events.
   * 
   * @param id       The primary key
   * @param apiKeyId The api key id
   * @param action   The billing action
   * @param address  The ip address
   * @param instant  The time of the action
   */
  public ApiKeyLog(Long id, Long apiKeyId, String action, String address, OffsetDateTime instant) {
    super(id, action, instant);
    this.apiKeyId = apiKeyId;
    this.address = address;
  }
}
