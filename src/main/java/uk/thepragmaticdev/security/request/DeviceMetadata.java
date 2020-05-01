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
public class DeviceMetadata {

  @EqualsAndHashCode.Include
  @CsvBindByName
  private String operatingSystemFamily;

  @EqualsAndHashCode.Include
  @CsvBindByName
  private String operatingSystemMajor;

  @EqualsAndHashCode.Include
  @CsvBindByName
  private String operatingSystemMinor;

  @EqualsAndHashCode.Include
  @CsvBindByName
  private String userAgentFamily;

  @EqualsAndHashCode.Include
  @CsvBindByName
  private String userAgentMajor;

  @EqualsAndHashCode.Include
  @CsvBindByName
  private String userAgentMinor;
}