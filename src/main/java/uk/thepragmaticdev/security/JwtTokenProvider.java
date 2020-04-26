package uk.thepragmaticdev.security;

import static java.util.Objects.nonNull;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import uk.thepragmaticdev.account.Role;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AccountCode;

@Component
public class JwtTokenProvider {

  // TODO INSECURE! Static key should be kept on a config-server.
  private String jwtSecretKey;

  private long validityInMilliseconds;

  private MyUserDetails myUserDetails;

  /**
   * TODO.
   * 
   * @param jwtSecretKey           TODO
   * @param validityInMilliseconds TODO
   * @param myUserDetails          TODO
   */
  @Autowired
  public JwtTokenProvider(//
      @Value("${security.jwt.token.secret-key}") String jwtSecretKey, //
      @Value("${security.jwt.token.expire-length}") long validityInMilliseconds, //
      MyUserDetails myUserDetails) {
    this.jwtSecretKey = jwtSecretKey;
    this.validityInMilliseconds = validityInMilliseconds;
    this.myUserDetails = myUserDetails;
  }

  @PostConstruct
  protected void init() {
    jwtSecretKey = Base64.getEncoder().encodeToString(jwtSecretKey.getBytes());
  }

  /**
   * TODO.
   * 
   * @param username TODO
   * @param roles    TODO
   * @return
   */
  public String createToken(String username, List<Role> roles) {
    var now = new Date();
    var validity = new Date(now.getTime() + validityInMilliseconds);
    var claims = Jwts.claims().setSubject(username);

    claims.put("auth", roles.stream().map(s -> new SimpleGrantedAuthority(s.getAuthority())).filter(Objects::nonNull)
        .collect(Collectors.toList()));
    return Jwts.builder() //
        .setClaims(claims) //
        .setIssuedAt(now) //
        .setExpiration(validity) //
        .signWith(SignatureAlgorithm.HS256, jwtSecretKey) //
        .compact();
  }

  /**
   * TODO.
   * 
   * @param req TODO
   * @return TODO
   */
  public String resolveToken(HttpServletRequest req) {
    var bearerToken = req.getHeader("Authorization");
    if (nonNull(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  /**
   * TODO.
   * 
   * @param token TODO
   * @return
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser().setSigningKey(jwtSecretKey).parseClaimsJws(token);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      throw new ApiException(AccountCode.INVALID_EXPIRED_TOKEN);
    }
  }

  /**
   * TODO.
   * 
   * @param token TODO
   * @return
   */
  public Authentication getAuthentication(String token) {
    var userDetails = myUserDetails.loadUserByUsername(getUsername(token));
    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }

  /**
   * TODO.
   * 
   * @param token TODO
   * @return
   */
  public String getUsername(String token) {
    return Jwts.parser().setSigningKey(jwtSecretKey).parseClaimsJws(token).getBody().getSubject();
  }
}