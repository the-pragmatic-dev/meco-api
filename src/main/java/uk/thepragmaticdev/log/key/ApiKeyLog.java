package uk.thepragmaticdev.log.key;

import com.opencsv.bean.CsvIgnore;
import com.opencsv.bean.CsvRecurse;
import java.time.OffsetDateTime;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.log.Log;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Data
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class ApiKeyLog extends Log implements Model {

  @ManyToOne
  @CsvIgnore
  private ApiKey apiKey;

  @Embedded
  @CsvRecurse
  private RequestMetadata requestMetadata; // generated

  /**
   * Entity to capture api key events.
   * 
   * @param id              The primary key
   * @param apiKey          The authenticated api key
   * @param action          The billing action
   * @param requestMetadata The geolocation and ip of the request
   * @param createdDate     The time of the action
   */
  public ApiKeyLog(Long id, ApiKey apiKey, String action, RequestMetadata requestMetadata, OffsetDateTime createdDate) {
    super(id, action, createdDate);
    this.apiKey = apiKey;
    this.requestMetadata = requestMetadata;
  }
}
