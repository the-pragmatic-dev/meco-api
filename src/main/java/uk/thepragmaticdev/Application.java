package uk.thepragmaticdev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class Application {

  /**
   * TODO.
   * 
   * @param args TODO
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
