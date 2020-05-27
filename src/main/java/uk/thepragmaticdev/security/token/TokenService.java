package uk.thepragmaticdev.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.Role;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AuthCode;
import uk.thepragmaticdev.security.UserService;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Service
public class TokenService {

  private final UserService userService;

  private final RefreshTokenRepository refreshTokenRepository;

  private String secretKey;

  private final long accessTokenExpiration;

  private final long refreshTokenExpiration;

  /**
   * Service for creating, resolving, validating and parsing access and refresh
   * tokens.
   * 
   * @param userService            The service for loading a user based on
   *                               username
   * @param refreshTokenRepository The data access repository for refresh tokens
   * @param secretKey              Signing key to use to digitally sign the JWT
   * @param accessTokenExpiration  Sets the access token expiration minutes
   * @param refreshTokenExpiration Sets the refresh token expiration days
   */
  @Autowired
  public TokenService(//
      UserService userService, //
      RefreshTokenRepository refreshTokenRepository, //
      @Value("${security.token.secret-key}") String secretKey, //
      @Value("${security.token.access-token-expiration-minutes}") long accessTokenExpiration, //
      @Value("${security.token.refresh-token-expiration-days}") long refreshTokenExpiration) {
    this.userService = userService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  /**
   * Actually signs and builds the JWT access token and serializes it to a
   * compact, URL-safe string according to the JWT Compact Serialization rules.
   * 
   * @param username The username of an account accessing resources
   * @param roles    The roles owned by the account
   * @return A compact URL-safe JWT string
   */
  public String createAccessToken(String username, List<Role> roles) {
    var now = new Date();
    var expiration = new Date(OffsetDateTime.now().plusMinutes(accessTokenExpiration).toInstant().toEpochMilli());
    var claims = Jwts.claims().setSubject(username);
    // iterate account roles and add each one as a granted authority
    claims.put("auth", roles.stream().map(r -> new SimpleGrantedAuthority(r.getAuthority())).filter(Objects::nonNull)
        .collect(Collectors.toList()));
    return Jwts.builder() //
        .setClaims(claims) //
        .setIssuedAt(now) //
        .setExpiration(expiration) //
        .signWith(SignatureAlgorithm.HS256, secretKey) //
        .compact();
  }

  /**
   * Generates a universally unique refresh token. The refresh token is used to
   * generate expired access tokens. Request metadata is associated with the token
   * to ensure refresh comes from the same client.
   * 
   * @param requestMetadata The geolocation and device metadata
   * @return A universally unique refresh token
   */
  public UUID createRefreshToken(Account account, RequestMetadata requestMetadata) {
    var token = UUID.randomUUID();
    var refreshToken = new RefreshToken(token, OffsetDateTime.now().plusDays(refreshTokenExpiration), requestMetadata,
        account);
    refreshTokenRepository.save(refreshToken);
    return refreshToken.getToken();
  }

  /**
   * Reads the authorization request header and checks if a bearer token exists.
   * If a token exists, the bearer prefix is stripped and the jwt is returned.
   * 
   * @param request The request information for HTTP servlets
   * @return An existsing JWT, otherwise null
   */
  public String resolveToken(HttpServletRequest request) {
    var prefix = "Bearer ";
    var bearerToken = request.getHeader("Authorization");
    return (bearerToken != null && bearerToken.startsWith(prefix)) ? bearerToken.substring(prefix.length()) : null;
  }

  /**
   * Checks if a given token is present and valid by parsing the claims with the
   * secret signing key. An expired token is not valid.
   * 
   * @param token A compact URL-safe JWT string
   * @return true if the token is valid otherwise false
   */
  public boolean validateToken(String token) {
    if (token == null) {
      return false;
    }
    return parseJwsClaims(token, false) != null;
  }

  /**
   * Parse a JSON web signature using our secret key and retrieve its claims.
   * Claims of an expired token can still be retrieved if allowExpired is set to
   * true.
   * 
   * @param token        A compact URL-safe JWT string
   * @param allowExpired Will allow returning of claims if token has expired
   * @return A map of JWT claims
   */
  public Claims parseJwsClaims(String token, boolean allowExpired) {
    try {
      return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    } catch (ExpiredJwtException ex) {
      if (allowExpired) {
        return ex.getClaims();
      }
      throw new ApiException(AuthCode.INVALID_EXPIRED_TOKEN);
    } catch (JwtException ex) {
      throw new ApiException(AuthCode.INVALID_EXPIRED_TOKEN);
    }
  }

  /**
   * Loads an account by username and transforms this into a user record. The user
   * record simply stores user information which is encapsulated into an
   * authentication token.
   * 
   * @param token A compact URL-safe JWT string
   * @return An authentication token containing the principal and authorities
   */
  public Authentication getAuthentication(String token) {
    var userDetails = userService.loadUserByUsername(parseUsername(token));
    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }

  private String parseUsername(String token) {
    return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
  }
}