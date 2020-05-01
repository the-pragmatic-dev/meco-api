package uk.thepragmaticdev.security.request;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvRecurse;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class RequestMetadata {

  @CsvBindByName
  private String ip;

  @Embedded
  @CsvRecurse
  private GeoMetadata geoMetadata;

  @Embedded
  @CsvRecurse
  private DeviceMetadata deviceMetadata;
}