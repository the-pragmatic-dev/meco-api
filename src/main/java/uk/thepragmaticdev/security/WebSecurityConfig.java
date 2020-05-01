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

  private final JwtTokenProvider jwtTokenProvider;

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
    // disable cross site request forgery
    http.csrf().disable();

    // add corsfilter to bypass authorization checks for preflight options requests.
    http.cors();

    // no sessions will be created or used by spring security
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    // entry points
    http.authorizeRequests()
        // TODO: might remove swagger
        // allow swagger api docs endpoint
        .antMatchers("/v3/api-docs", "/configuration/**", "/swagger*/**", "/webjars/**").permitAll()
        // allow actuator endpoint
        .antMatchers("/actuator/**").permitAll()
        // allow account signin and account signup endpoints
        .antMatchers("/accounts/signin").permitAll()//
        .antMatchers("/accounts/signup").permitAll()//
        .antMatchers("/accounts/me/forgot").permitAll()//
        .antMatchers("/accounts/me/reset").permitAll()
        // disallow everything else
        .anyRequest().authenticated();

    // if a user tries to access a resource without having enough permissions
    http.exceptionHandling().accessDeniedPage("/login");

    // apply jwt filter
    http.apply(new JwtTokenFilterConfigurer(jwtTokenProvider));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}