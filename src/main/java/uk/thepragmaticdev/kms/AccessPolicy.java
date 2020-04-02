package uk.thepragmaticdev.kms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.api.Model;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class AccessPolicy implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  private Long id;

  @Size(min = 1)
  private String name;

  @Size(min = 1)
  private String range;

  @ManyToOne
  @JsonIgnore
  private ApiKey apiKey;
}
