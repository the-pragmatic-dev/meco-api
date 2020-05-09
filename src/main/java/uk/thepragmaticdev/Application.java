package uk.thepragmaticdev;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@SpringBootApplication
@EnableAspectJAutoProxy
public class Application {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);

  private final RequestMetadataService requestMetadataService;

  @Autowired
  public Application(RequestMetadataService requestMetadataService) {
    this.requestMetadataService = requestMetadataService;
  }

  /**
   * Launches the application.
   * 
   * @param args application startup arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  /**
   * Runs once on start up and attempts to load GeoLite2 database. Exits
   * application if database is unable to load.
   */
  @PostConstruct
  public void init() {
    if (!requestMetadataService.loadDatabase()) {
      LOG.error(CriticalCode.GEOLITE_DOWNLOAD_ERROR.getMessage());
      throw new ApiException(CriticalCode.GEOLITE_DOWNLOAD_ERROR);
    }
  }
}
