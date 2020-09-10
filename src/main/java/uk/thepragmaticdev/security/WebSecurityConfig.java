package uk.thepragmaticdev.security;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.thepragmaticdev.exception.ApiError;
import uk.thepragmaticdev.exception.code.AuthCode;
import uk.thepragmaticdev.kms.ApiKeyService;
import uk.thepragmaticdev.security.key.ApiKeyFilterConfigurer;
import uk.thepragmaticdev.security.token.TokenFilterConfigurer;
import uk.thepragmaticdev.security.token.TokenService;

@Configuration
@Log4j2
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private final ApiKeyService apiKeyService;

  private final TokenService tokenService;

  @Autowired
  public WebSecurityConfig(@Lazy ApiKeyService apiKeyService, TokenService tokenService) {
    this.apiKeyService = apiKeyService;
    this.tokenService = tokenService;
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // disable cross site request forgery
    http.csrf().disable();

    // add corsfilter to bypass authorization checks for preflight options requests.
    http.cors();

    // no sessions will be created or used by spring security
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    // entry points
    http.authorizeRequests()
        // allow actuator endpoint
        .antMatchers("/actuator/**").permitAll()
        // allow public endpoints
        .antMatchers("/v1/auth/signin").permitAll()//
        .antMatchers("/v1/auth/signup").permitAll()//
        .antMatchers("/v1/auth/forgot").permitAll()//
        .antMatchers("/v1/auth/reset").permitAll()//
        .antMatchers("/v1/auth/refresh").permitAll()//
        .antMatchers("/v1/billing/prices").permitAll()
        // disallow everything else
        .anyRequest().authenticated();

    // authentication error response
    http.exceptionHandling().authenticationEntryPoint((req, res, e) -> authenticationEntryPoint(res));

    // if a user tries to access a resource without having enough permissions
    http.exceptionHandling().accessDeniedPage("/login");

    // apply access token filter
    http.apply(new TokenFilterConfigurer(tokenService));

    // apply api key filter
    http.apply(new ApiKeyFilterConfigurer(apiKeyService));
  }

  private void authenticationEntryPoint(HttpServletResponse response) throws IOException {
    var responseBody = new ApiError(//
        AuthCode.AUTH_HEADER_INVALID.getStatus(), //
        AuthCode.AUTH_HEADER_INVALID.getMessage() //
    );
    log.warn("{}", responseBody);
    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(AuthCode.AUTH_HEADER_INVALID.getStatus().value());
    response.getWriter().write(responseBody.toString());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}