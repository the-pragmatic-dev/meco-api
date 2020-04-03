package uk.thepragmaticdev;

import java.text.SimpleDateFormat;
import java.util.Date;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

public class RestTest {

  /**
   * Endpoints.
   */
  protected static final String ANALYTICS_ENDPOINT = "http://localhost:8080/api/analytics/";
  protected static final String HORSES_ENDPOINT = "http://localhost:8080/api/horses/";
  protected static final String METADATA_ENDPOINT = "http://localhost:8080/api/metadata/";
  protected static final String RIDES_ENDPOINT = "http://localhost:8080/api/rides/";
  protected static final String RIDERS_ENDPOINT = "http://localhost:8080/api/riders/";
  protected static final String SETTINGS_ENDPOINT = "http://localhost:8080/api/settings/";

  /**
   * Dirty models.
   */
  private <T> T podam(Class<T> pojoClass) {
    PodamFactory factory = new PodamFactoryImpl();
    factory.getStrategy().addOrReplaceTypeManufacturer(Date.class, new DateManufacturer());
    return factory.manufacturePojoWithFullData(pojoClass);
  }

  /**
   * Formatting helpers.
   */
  public String format(Date dateToFormat) {
    return new SimpleDateFormat("yyy-MM-dd").format(dateToFormat);
  }
}
