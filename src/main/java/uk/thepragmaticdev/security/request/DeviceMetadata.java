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
public class DeviceMetadata {

  @EqualsAndHashCode.Include
  private String operatingSystemFamily;

  @EqualsAndHashCode.Include
  private String operatingSystemMajor;

  @EqualsAndHashCode.Include
  private String operatingSystemMinor;

  @EqualsAndHashCode.Include
  private String userAgentFamily;

  @EqualsAndHashCode.Include
  private String userAgentMajor;

  @EqualsAndHashCode.Include
  private String userAgentMinor;
}