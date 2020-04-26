package uk.thepragmaticdev.security.request;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GeoMetadata {

  @EqualsAndHashCode.Include
  private String cityName;

  @EqualsAndHashCode.Include
  private String countryIsoCode;

  @EqualsAndHashCode.Include
  private String subdivisionIsoCode;
}