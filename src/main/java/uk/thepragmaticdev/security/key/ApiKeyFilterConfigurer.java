package uk.thepragmaticdev.security.key;

import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uk.thepragmaticdev.kms.ApiKeyService;

public class ApiKeyFilterConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

  private final ApiKeyService apiKeyService;

  public ApiKeyFilterConfigurer(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    var customFilter = new ApiKeyFilter(apiKeyService);
    http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
  }
}