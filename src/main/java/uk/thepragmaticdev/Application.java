package uk.thepragmaticdev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class Application {

  /**
   * Launches the application.
   * 
   * @param args application startup arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
