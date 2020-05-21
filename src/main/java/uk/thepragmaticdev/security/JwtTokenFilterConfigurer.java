package uk.thepragmaticdev.security;

import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class JwtTokenFilterConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

  private final JwtTokenService jwtTokenService;

  public JwtTokenFilterConfigurer(JwtTokenService jwtTokenService) {
    this.jwtTokenService = jwtTokenService;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    var customFilter = new JwtTokenFilter(jwtTokenService);
    http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
  }

}