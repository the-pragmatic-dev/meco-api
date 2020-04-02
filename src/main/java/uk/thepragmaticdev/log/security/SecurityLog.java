package uk.thepragmaticdev.log.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.CsvIgnore;
import java.time.OffsetDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.api.Model;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class SecurityLog implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  @CsvIgnore
  private Long id;

  @CsvIgnore
  private Long accountId;

  private String action; // generated

  private String address; // generated

  private OffsetDateTime instant; // generated
}
