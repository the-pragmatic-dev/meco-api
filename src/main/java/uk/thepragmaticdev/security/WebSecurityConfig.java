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

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  public WebSecurityConfig(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    // Disable Cross Site Request Forgery
    http.csrf().disable();

    // Adds a CorsFilter to bypass the authorization checks for preflight OPTIONS
    // requests.
    http.cors();

    // No sessions will be created or used by Spring Security
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    // Entry points
    http.authorizeRequests()
        // Allow Swagger API Docs endpoint
        .antMatchers("/v3/api-docs", "/configuration/**", "/swagger*/**", "/webjars/**").permitAll()
        // Allow Actuator endpoint
        .antMatchers("/actuator/**").permitAll()
        // Allow Account Signin and Account Signup endpoints
        .antMatchers("/accounts/signin").permitAll()//
        .antMatchers("/accounts/signup").permitAll()
        // Disallow everything else
        .anyRequest().authenticated();

    // If a user tries to access a resource without having enough permissions
    http.exceptionHandling().accessDeniedPage("/login");

    // Apply JWT
    http.apply(new JwtTokenFilterConfigurer(jwtTokenProvider));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

}