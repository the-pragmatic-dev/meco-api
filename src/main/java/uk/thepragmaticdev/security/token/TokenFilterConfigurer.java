package uk.thepragmaticdev.security.token;

import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class TokenFilterConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

  private final TokenService tokenService;

  public TokenFilterConfigurer(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    var customFilter = new TokenFilter(tokenService);
    http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
  }

}