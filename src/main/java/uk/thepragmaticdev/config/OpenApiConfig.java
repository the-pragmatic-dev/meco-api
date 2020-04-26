package uk.thepragmaticdev.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  /**
   * Provides configuarion for OpenAPI instance.
   * 
   * @return The OpenAPI instance
   */
  @Bean
  public OpenAPI api() {
    return new OpenAPI() //
        .info(new Info() //
            .title("Monitoring Explicit Content Online") //
            .version("1.0.0") //
            .description("Account, billing & key management services.") //
            .license(new License() //
                .name("Apache License Version 2.0") //
                .url("http://www.apache.org/licenses/") //
            ) //
            .contact(new Contact() //
                .email("steve@thepragmaticdev.uk") //
                .name("Stephen Cathcart") //
                .url("https://thepragmaticdev.uk/"))) //
        .externalDocs(new ExternalDocumentation() //
            .description("https://swagger.io/") //
            .url("http://swagger.io") //
        ) //
        .components(new Components() //
            .addSecuritySchemes("JWT", new SecurityScheme() //
                .type(Type.HTTP) //
                .scheme("bearer") //
                .bearerFormat("JWT") //
            ) //
        ) //
        .addSecurityItem(new SecurityRequirement().addList("JWT"));
  }
}
