package uk.thepragmaticdev.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.thepragmaticdev.security.token.TokenFilterConfigurer;
import uk.thepragmaticdev.security.token.TokenService;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private final TokenService tokenService;

  @Autowired
  public WebSecurityConfig(TokenService tokenService) {
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
        .antMatchers("/accounts/signin").permitAll()//
        .antMatchers("/accounts/signup").permitAll()//
        .antMatchers("/accounts/me/forgot").permitAll()//
        .antMatchers("/accounts/me/reset").permitAll()//
        .antMatchers("/billing/prices").permitAll()
        // disallow everything else
        .anyRequest().authenticated();

    // if a user tries to access a resource without having enough permissions
    http.exceptionHandling().accessDeniedPage("/login");

    // apply token filter
    http.apply(new TokenFilterConfigurer(tokenService));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}