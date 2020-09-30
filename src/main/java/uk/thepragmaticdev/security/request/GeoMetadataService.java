package uk.thepragmaticdev.security.request;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import java.io.IOException;

public interface GeoMetadataService {

  GeoMetadata extractGeoMetadata(String host, DatabaseReader databaseReader) throws IOException, GeoIp2Exception;
}
