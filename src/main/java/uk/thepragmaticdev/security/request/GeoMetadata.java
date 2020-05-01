package uk.thepragmaticdev.security.request;

import com.opencsv.bean.CsvBindByName;
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
  @CsvBindByName
  private String cityName;

  @EqualsAndHashCode.Include
  @CsvBindByName
  private String countryIsoCode;

  @EqualsAndHashCode.Include
  @CsvBindByName
  private String subdivisionIsoCode;
}