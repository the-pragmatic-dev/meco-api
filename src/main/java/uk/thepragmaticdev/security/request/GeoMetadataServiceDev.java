package uk.thepragmaticdev.security.request;

import com.maxmind.geoip2.DatabaseReader;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod & !integration")
public class GeoMetadataServiceDev implements GeoMetadataService {

  @Override
  public GeoMetadata extractGeoMetadata(String host, DatabaseReader databaseReader) {
    return new GeoMetadata("London", "GB", "ENG");
  }
}
