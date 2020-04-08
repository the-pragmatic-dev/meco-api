package uk.thepragmaticdev.kms;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import uk.thepragmaticdev.endpoint.Model;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class Scope implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  private Long id;

  @Column(columnDefinition = "boolean default false")
  private Boolean image;

  @Column(columnDefinition = "boolean default false")
  private Boolean gif;

  @Column(columnDefinition = "boolean default false")
  private Boolean text;

  @Column(columnDefinition = "boolean default false")
  private Boolean video;

  @OneToOne(mappedBy = "scope")
  @JsonIgnore
  private ApiKey apiKey;
}
