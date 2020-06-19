package uk.thepragmaticdev.security.key;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.thepragmaticdev.exception.ApiError;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AuthCode;
import uk.thepragmaticdev.kms.ApiKeyService;

@Log4j2
public class ApiKeyFilter extends OncePerRequestFilter {

  private final ApiKeyService apiKeyService;

  public ApiKeyFilter(ApiKeyService tokenService) {
    this.apiKeyService = tokenService;
  }

  @Override
  protected void doFilterInternal(//
      HttpServletRequest httpServletRequest, //
      HttpServletResponse httpServletResponse, //
      FilterChain filterChain) throws ServletException, IOException {
    var rawApiKey = apiKeyService.extract(httpServletRequest);
    try {
      var authenticatedApiKey = apiKeyService.authenticate(rawApiKey);
      if (authenticatedApiKey.isPresent()) {
        var auth = new ApiKeyAuthenticationToken(authenticatedApiKey.get());
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    } catch (ApiException ex) {
      SecurityContextHolder.clearContext();

      var responseBody = new ApiError(//
          AuthCode.API_KEY_INVALID.getStatus(), //
          AuthCode.API_KEY_INVALID.getMessage() //
      );
      log.warn("{}", responseBody);
      httpServletResponse.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
      httpServletResponse.setStatus(AuthCode.API_KEY_INVALID.getStatus().value());
      httpServletResponse.getWriter().write(responseBody.toString());
      return;
    }
    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }
}