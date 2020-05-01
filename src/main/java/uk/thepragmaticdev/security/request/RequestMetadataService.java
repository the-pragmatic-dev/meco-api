package uk.thepragmaticdev.security.request;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.rauschig.jarchivelib.ArchiveEntry;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ua_parser.Parser;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.log.security.SecurityLog;
import uk.thepragmaticdev.log.security.SecurityLogService;

@Service
public class RequestMetadataService {

  private static final Logger logger = LoggerFactory.getLogger(RequestMetadataService.class);

  private final EmailService emailService;

  private final SecurityLogService securityLogService;

  private final String databaseName;

  private final String databaseUrl;

  private final String databaseDirectory;

  private DatabaseReader databaseReader;

  /**
   * Service for extracting and verifying client geolocation and device metadata
   * from a given request.
   * 
   * @param databaseName       The GeoLite2 database name
   * @param databaseUrl        The GeoLite2 database remote url
   * @param databaseDirectory  The local directory of the GeoLite2 database
   * @param emailService       The service for sending emails
   * @param securityLogService The service for finding security logs
   */
  public RequestMetadataService(//
      @Value("${geolite2.name}") String databaseName, //
      @Value("${geolite2.permalink}") String databaseUrl, //
      @Value("${geolite2.directory}") String databaseDirectory, //
      EmailService emailService, //
      @Lazy SecurityLogService securityLogService) {
    this.databaseName = databaseName;
    this.databaseUrl = databaseUrl;
    this.databaseDirectory = databaseDirectory;
    this.emailService = emailService;
    this.securityLogService = securityLogService;
  }

  /**
   * Load a GeoLite2 database from an existing local file. If a local file does
   * not exist it will attempt to download the database from an origin server.
   * 
   * @return True if database loaded successfully
   */
  public boolean loadDatabase() {
    try {
      var database = fetchDatabase().orElseThrow(() -> new ApiException(CriticalCode.GEOLITE_DOWNLOAD_ERROR));
      this.databaseReader = new DatabaseReader.Builder(database.toFile()).build();
      logger.info("GeoLite2 database loaded");
    } catch (IOException ex) {
      logger.error("Failed to load GeoLite2 database: {}", ex.getMessage());
      return false;
    }
    return true;
  }

  private Optional<Path> fetchDatabase() throws MalformedURLException, IOException {
    final var connectionTimeout = 10000;
    final var readTimeout = 10000;
    var localDatabase = findLocalDatabase();
    if (!localDatabase.isPresent()) {
      logger.info("Downloading GeoLite2 database from remote");
      var extension = ".tar.gz";
      var destination = Paths.get(databaseDirectory.concat(databaseName).concat(extension));
      FileUtils.copyURLToFile(new URL(databaseUrl), destination.toFile(), connectionTimeout, readTimeout);
      localDatabase = extractDatabase(destination);
    }
    return localDatabase;
  }

  private Optional<Path> findLocalDatabase() throws IOException {
    return Files.find(Paths.get(databaseDirectory), Integer.MAX_VALUE,
        (path, basicFileAttributes) -> path.toFile().getName().matches(databaseName)).findFirst();
  }

  private Optional<Path> extractDatabase(Path destination) throws IOException {
    var archiver = ArchiverFactory.createArchiver(destination.toFile());
    var stream = archiver.stream(destination.toFile());
    ArchiveEntry entry;
    while (nonNull(entry = stream.getNextEntry())) {
      if (entry.getName().contains(databaseName)) {
        logger.info("Found database: {}", entry.getName());
        entry.extract(destination.getParent().toFile());
        return Optional.of(destination.getParent().resolve(entry.getName()));
      }
    }
    stream.close();
    return Optional.empty();
  }

  /**
   * Extracts client geolocation and device metadata from the request. If the
   * request does not match an existing request we send an email notification.
   * 
   * @param account The account in which to verify request
   * @param request The request information for HTTP servlets
   */
  public void verifyRequest(Account account, HttpServletRequest request) {
    // TODO: Unit test
    extractRequestMetadata(request).ifPresent(requestMetadata -> verifyRequestMetadata(account, requestMetadata));
  }

  private void verifyRequestMetadata(Account account, RequestMetadata requestMetadata) {
    if (!matchesExistingRequestMetadata(account, requestMetadata)) {
      SecurityLog log = securityLogService.unrecognizedDevice(account, requestMetadata);
      emailService.sendUnrecognizedDevice(account, log);
      logger.warn("Unrecognized device {} detected for account {}", requestMetadata, account.getId());
    }
  }

  private boolean matchesExistingRequestMetadata(Account account, RequestMetadata requestMetadata) {
    var securityLogs = securityLogService.findAllByAccountId(account);
    return securityLogs.stream().anyMatch(log -> metaDataMatches(requestMetadata, log.getRequestMetadata()));
  }

  private boolean metaDataMatches(RequestMetadata existing, RequestMetadata request) {
    return Objects.equals(existing.getGeoMetadata(), request.getGeoMetadata())
        && Objects.equals(existing.getDeviceMetadata(), request.getDeviceMetadata());
  }

  /**
   * Finds client geolocation and device metadata from the given request. If an
   * error occurs it will return empty.
   * 
   * @param request The request information for HTTP servlets
   * @return The geolocation and device metadata
   */
  public Optional<RequestMetadata> extractRequestMetadata(HttpServletRequest request) {
    // TODO: Unit test
    try {
      var geoMetadata = extractGeoMetadata(request);
      var deviceMetadata = extractDeviceMetadata(request);
      return Optional.of(new RequestMetadata(//
          extractIp(request), //
          geoMetadata, //
          deviceMetadata));
    } catch (AddressNotFoundException ex) {
      logger.warn("IP address not present in the database: ", ex.getMessage());
    } catch (GeoIp2Exception ex) {
      logger.warn("Generic GeoIp2 error: ", ex.getMessage());
    } catch (UnknownHostException ex) {
      logger.warn("IP address of host could not be determined: ", ex.getMessage());
    } catch (IOException ex) {
      logger.warn("IO error when extracting request metadata: ", ex.getMessage());
    }
    return Optional.empty();
  }

  private GeoMetadata extractGeoMetadata(HttpServletRequest request)
      throws GeoIp2Exception, UnknownHostException, IOException {
    var ipAddress = InetAddress.getByName(extractIp(request));
    var response = databaseReader.city(ipAddress);
    return new GeoMetadata(//
        isNull(response.getCity()) ? "" : response.getCity().getName(), //
        isNull(response.getCountry()) ? "" : response.getCountry().getIsoCode(), //
        isNull(response.getLeastSpecificSubdivision()) ? "" : response.getLeastSpecificSubdivision().getIsoCode());
  }

  private DeviceMetadata extractDeviceMetadata(HttpServletRequest request) throws IOException {
    var client = new Parser().parse(request.getHeader("user-agent"));
    // a clients os and userAgent will never be null
    return new DeviceMetadata(//
        client.os.family, //
        client.os.major, //
        client.os.minor, //
        client.userAgent.family, //
        client.userAgent.major, //
        client.userAgent.minor//
    );
  }

  private String extractIp(HttpServletRequest request) {
    var clientXForwardedForIp = request.getHeader("X-FORWARDED-FOR");
    if (isNull(clientXForwardedForIp)) {
      return request.getRemoteAddr();
    } else {
      return parseXForwardedHeader(clientXForwardedForIp);
    }
  }

  private String parseXForwardedHeader(String header) {
    return header.split(" *, *")[0];
  }
}