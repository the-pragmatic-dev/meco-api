package uk.thepragmaticdev.kms.usage;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.kms.ApiKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class ApiKeyUsage implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate usageDate;

  private Long textOperations;

  private Long imageOperations;

  @ManyToOne
  @ToString.Exclude
  private ApiKey apiKey;
}
