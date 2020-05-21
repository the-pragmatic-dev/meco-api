package uk.thepragmaticdev.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.account.Role;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AccountCode;

@Service
public class JwtTokenService {

  private final long validityInMilliseconds;

  private final UserService userService;

  private String secretKey;

  /**
   * Service for creating, resolving, validating and parsing JWTs.
   * 
   * @param secretKey              Signing key to use to digitally sign the JWT
   * @param validityInMilliseconds Sets the JWT claims expiration value
   * @param userService            The service for loading a user based on
   *                               username
   */
  @Autowired
  public JwtTokenService(//
      @Value("${security.jwt.token.secret-key}") String secretKey, //
      @Value("${security.jwt.token.expire-length}") long validityInMilliseconds, //
      UserService userService) {
    this.secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    this.validityInMilliseconds = validityInMilliseconds;
    this.userService = userService;
  }

  /**
   * Actually signs and builds the JWT and serializes it to a compact, URL-safe
   * string according to the JWT Compact Serialization rules.
   * 
   * @param username The username of an account accessing resources
   * @param roles    The roles owned by the account
   * @return A compact URL-safe JWT string.
   */
  public String createToken(String username, List<Role> roles) {
    var now = new Date();
    var validity = new Date(now.getTime() + validityInMilliseconds);
    var claims = Jwts.claims().setSubject(username);
    // iterate account roles and add each one as a granted authority
    claims.put("auth", roles.stream().map(r -> new SimpleGrantedAuthority(r.getAuthority())).filter(Objects::nonNull)
        .collect(Collectors.toList()));
    return Jwts.builder() //
        .setClaims(claims) //
        .setIssuedAt(now) //
        .setExpiration(validity) //
        .signWith(SignatureAlgorithm.HS256, secretKey) //
        .compact();
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
   * secret signing key and checking if the token has expired.
   * 
   * @param token A compact URL-safe JWT string
   * @return true if the token is valid otherwise false
   */
  public boolean isValid(String token) {
    if (token == null) {
      return false;
    }
    try {
      var claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
      if (claims.getBody().getExpiration().before(new Date())) {
        return false;
      }
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      throw new ApiException(AccountCode.INVALID_EXPIRED_TOKEN);
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