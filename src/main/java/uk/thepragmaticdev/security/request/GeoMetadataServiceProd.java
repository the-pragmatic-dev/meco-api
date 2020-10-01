package uk.thepragmaticdev.security.request;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import java.io.IOException;
import java.net.InetAddress;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod | integration")
public class GeoMetadataServiceProd implements GeoMetadataService {

  @Override
  public GeoMetadata extractGeoMetadata(String host, DatabaseReader databaseReader)
      throws IOException, GeoIp2Exception {
    var ipAddress = InetAddress.getByName(host);
    var response = databaseReader.city(ipAddress);
    return new GeoMetadata(//
        response.getCity() == null ? "" : response.getCity().getName(), //
        response.getCountry() == null ? "" : response.getCountry().getIsoCode(), //
        response.getLeastSpecificSubdivision() == null ? "" : response.getLeastSpecificSubdivision().getIsoCode());
  }

}
