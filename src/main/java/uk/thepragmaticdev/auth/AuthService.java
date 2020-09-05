package uk.thepragmaticdev.auth;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.account.Role;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AccountCode;
import uk.thepragmaticdev.exception.code.AuthCode;
import uk.thepragmaticdev.log.security.SecurityLogService;
import uk.thepragmaticdev.security.request.RequestMetadata;
import uk.thepragmaticdev.security.request.RequestMetadataService;
import uk.thepragmaticdev.security.token.TokenPair;
import uk.thepragmaticdev.security.token.TokenService;

@Service
public class AuthService {

  private final AccountService accountService;

  private final SecurityLogService securityLogService;

  private final EmailService emailService;

  private final RequestMetadataService requestMetadataService;

  private final PasswordEncoder passwordEncoder;

  private final TokenService tokenService;

  private final AuthenticationManager authenticationManager;

  /**
   * Service for creating, authorizing and updating accounts.
   * 
   * @param accountService         The service for retrieving account information
   * @param securityLogService     The service for accessing security logs
   * @param emailService           The service for sending emails
   * @param requestMetadataService The service for gathering ip and location
   *                               information
   * @param passwordEncoder        The service for encoding passwords
   * @param tokenService           The service for creating, validating tokens
   * @param authenticationManager  The manager for authentication providers
   */
  @Autowired
  public AuthService(//
      AccountService accountService, //
      SecurityLogService securityLogService, //
      EmailService emailService, //
      RequestMetadataService requestMetadataService, //
      PasswordEncoder passwordEncoder, //
      TokenService tokenService, //
      AuthenticationManager authenticationManager) {
    this.accountService = accountService;
    this.securityLogService = securityLogService;
    this.emailService = emailService;
    this.requestMetadataService = requestMetadataService;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.authenticationManager = authenticationManager;
  }

  /**
   * Authorize an account. If signing in from an unfamiliar ip or device the user
   * will be notified by email.
   * 
   * @param username The username of an account attemping to signin
   * @param password The password of an account attemping to signin
   * @param request  The request information for HTTP servlets
   * @return A token pair containing an access authentication token and a refresh
   *         token
   */
  public TokenPair signin(String username, String password, HttpServletRequest request) {
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
      var persistedAccount = accountService.findAuthenticatedAccount(username);
      var requestMetadata = requestMetadataService.verifyRequest(persistedAccount, request);
      return createTokenPair(persistedAccount, requestMetadata);
    } catch (AuthenticationException ex) {
      throw new ApiException(AuthCode.CREDENTIALS_INVALID);
    }
  }

  /**
   * Create a new account.
   * 
   * @param username The username of an account attemping to signup
   * @param password The password of an account attemping to signup
   * @return A token pair containing an access authentication token and a refresh
   *         token
   */
  @Transactional
  public TokenPair signup(String username, String password, HttpServletRequest request) {
    if (!accountService.existsByUsername(username)) {
      var persistedAccount = accountService.create(username, passwordEncoder.encode(password),
          Arrays.asList(Role.ROLE_ADMIN));
      securityLogService.created(persistedAccount);
      emailService.sendAccountCreated(persistedAccount);
      try {
        return createTokenPair(persistedAccount, requestMetadataService.extractRequestMetadata(request));
      } catch (ApiException ex) {
        throw new ApiException(AuthCode.REQUEST_METADATA_INVALID);
      }
    } else {
      throw new ApiException(AccountCode.USERNAME_UNAVAILABLE);
    }
  }

  /**
   * Send a forgotten password email to the requested account. The reset token is
   * valid for 24hours.
   * 
   * @param username A valid account username
   */
  public void forgot(String username) {
    var account = accountService.createPasswordResetToken(username);
    emailService.sendForgottenPassword(account);
  }

  /**
   * Reset old password to new password for the requested account.
   * 
   * @param password The accounts new password
   * @param token    The generated password reset token from the /forgot endpoint
   */
  public void reset(String password, String token) {
    var account = accountService.resetPasswordResetToken(passwordEncoder.encode(password), token);
    securityLogService.reset(account);
    emailService.sendResetPassword(account);
  }

  /**
   * Refresh an expired access token. Uses the expired access token along with the
   * refresh token and request metadata to generate a new token.
   * 
   * @param accessToken  An expired access token
   * @param refreshToken A refresh token generated on signin or signup
   * @param request      The request information for HTTP servlets
   * @return A valid access authentication token
   */
  public String refresh(String accessToken, UUID refreshToken, HttpServletRequest request) {
    var username = tokenService.parseJwsClaims(accessToken, true).getSubject();
    var account = accountService.findAuthenticatedAccount(username);
    var persistedRefreshToken = account.getRefreshTokens().stream().filter(r -> r.getToken().equals(refreshToken))
        .findFirst().orElseThrow(() -> new ApiException(AuthCode.REFRESH_TOKEN_NOT_FOUND));
    if (persistedRefreshToken.getExpirationTime().isBefore(OffsetDateTime.now())) {
      throw new ApiException(AuthCode.REFRESH_TOKEN_EXPIRED);
    }
    var requestMetadata = requestMetadataService.extractRequestMetadata(request)
        .orElseThrow(() -> new ApiException(AuthCode.REQUEST_METADATA_INVALID));
    if (!requestMetadata.equals(persistedRefreshToken.getRequestMetadata())) {
      throw new ApiException(AuthCode.REQUEST_METADATA_INVALID);
    }
    return tokenService.createAccessToken(account.getUsername(), account.getRoles());
  }

  private TokenPair createTokenPair(Account account, Optional<RequestMetadata> requestMetadata) {
    if (requestMetadata.isEmpty()) {
      throw new ApiException(AuthCode.REQUEST_METADATA_INVALID);
    }
    return new TokenPair(tokenService.createAccessToken(account.getUsername(), account.getRoles()),
        tokenService.createRefreshToken(account, requestMetadata.get()));
  }
}