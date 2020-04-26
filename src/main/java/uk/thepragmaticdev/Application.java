package uk.thepragmaticdev;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@SpringBootApplication
@EnableAspectJAutoProxy
public class Application {

  private ConfigurableApplicationContext context;

  private RequestMetadataService requestMetadataService;

  @Autowired
  public Application(ConfigurableApplicationContext context, RequestMetadataService requestMetadataService) {
    this.context = context;
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
      System.exit(SpringApplication.exit(context));
    }
  }
}
