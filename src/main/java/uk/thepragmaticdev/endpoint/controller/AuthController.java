package uk.thepragmaticdev.endpoint.controller;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.auth.AuthService;
import uk.thepragmaticdev.auth.dto.request.AuthRefreshRequest;
import uk.thepragmaticdev.auth.dto.request.AuthResetRequest;
import uk.thepragmaticdev.auth.dto.request.AuthSigninRequest;
import uk.thepragmaticdev.auth.dto.request.AuthSignupRequest;
import uk.thepragmaticdev.auth.dto.response.AuthRefreshResponse;
import uk.thepragmaticdev.auth.dto.response.AuthSigninResponse;
import uk.thepragmaticdev.auth.dto.response.AuthSignupResponse;

@RestController
@RequestMapping("v1/auth")
@CrossOrigin("*")
public class AuthController {

  private final AuthService authService;

  private final ModelMapper modelMapper;

  /**
   * Endpoint for generating access and refresh tokens, and resetting credentials.
   * 
   * @param authService The service for creating, authorizing and updating
   *                    accounts.
   * @param modelMapper An entity to domain mapper
   */
  @Autowired
  public AuthController(AuthService authService, ModelMapper modelMapper) {
    this.authService = authService;
    this.modelMapper = modelMapper;
  }

  /**
   * Authorize an account.
   * 
   * @param request The request information for HTTP servlets
   * @param signin  The account details for signing in
   * @return A token pair containing an access authentication token and a refresh
   *         token
   */
  @PostMapping(value = "/signin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AuthSigninResponse signin(HttpServletRequest request, @Valid @RequestBody AuthSigninRequest signin) {
    var tokenPair = authService.signin(signin.getUsername(), signin.getPassword(), request);
    return modelMapper.map(tokenPair, AuthSigninResponse.class);
  }

  /**
   * Create a new account.
   * 
   * @param request The new account details to be created
   * @return A token pair containing an access authentication token and a refresh
   *         token
   */
  @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.CREATED)
  public AuthSignupResponse signup(HttpServletRequest request, @Valid @RequestBody AuthSignupRequest signup) {
    var tokenPair = authService.signup(signup.getUsername(), signup.getPassword(), request);
    return modelMapper.map(tokenPair, AuthSignupResponse.class);
  }

  /**
   * Send a forgotten password email to the requested account.
   * 
   * @param username A valid account username
   */
  @PostMapping(value = "/forgot")
  public void forgot(@RequestParam(value = "username", required = true) String username) {
    authService.forgot(username);
  }

  /**
   * Reset old password to new password for the requested account.
   * 
   * @param request An request containing the new password
   * @param token   The generated password reset token from the /forgot endpoint
   */
  @PostMapping(value = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void reset(@Valid @RequestBody AuthResetRequest request,
      @RequestParam(value = "token", required = true) String token) {
    authService.reset(request.getPassword(), token);
  }

  /**
   * Refresh an expired access token. Uses the expired access token along with the
   * refresh token and request metadata to generate a new token.
   * 
   * @return A valid access authentication token
   */
  @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.CREATED)
  public AuthRefreshResponse refresh(HttpServletRequest request, @Valid @RequestBody AuthRefreshRequest refresh) {
    var accessToken = authService.refresh(refresh.getAccessToken(), //
        UUID.fromString(refresh.getRefreshToken()), //
        request);
    return new AuthRefreshResponse(accessToken);
  }
}