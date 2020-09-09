package uk.thepragmaticdev.kms.scope;

import javax.persistence.Column;
import javax.persistence.Embedded;
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
import lombok.ToString;
import uk.thepragmaticdev.endpoint.Model;
import uk.thepragmaticdev.kms.ApiKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "id") })
public class Scope implements Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "boolean not null default false")
  private boolean image;

  @Column(columnDefinition = "boolean not null default false")
  private boolean gif;

  @Embedded
  private TextScope textScope;

  @Column(columnDefinition = "boolean not null default false")
  private boolean video;

  @OneToOne(mappedBy = "scope")
  @ToString.Exclude
  private ApiKey apiKey;
}
