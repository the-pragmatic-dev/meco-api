package uk.thepragmaticdev;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import uk.co.jemos.podam.api.AttributeMetadata;
import uk.co.jemos.podam.api.DataProviderStrategy;
import uk.co.jemos.podam.typeManufacturers.AbstractTypeManufacturer;

public class DateManufacturer extends AbstractTypeManufacturer<Date> {

  @Override
  public Date getType(DataProviderStrategy dps, AttributeMetadata am, Map<String, Type> map) {
    var minDay = (int) LocalDate.of(1900, 1, 1).toEpochDay();
    var maxDay = (int) LocalDate.of(3000, 1, 1).toEpochDay();
    var randomDay = minDay + new Random().nextInt(maxDay - minDay);

    return Date.from(Instant.from((LocalDate.ofEpochDay(randomDay).atStartOfDay(ZoneId.of("GMT")))));
  }
}
