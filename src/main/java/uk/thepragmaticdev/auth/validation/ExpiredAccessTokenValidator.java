package uk.thepragmaticdev.auth.validation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.security.token.TokenService;

public class ExpiredAccessTokenValidator implements ConstraintValidator<ExpiredAccessToken, String> {

  private final TokenService tokenService;

  @Autowired
  public ExpiredAccessTokenValidator(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public boolean isValid(String accessToken, ConstraintValidatorContext context) {
    // Return invalid if no access token exists
    if (StringUtils.isBlank(accessToken)) {
      return false;
    }
    try {
      var claims = tokenService.parseJwsClaims(accessToken, true);
      var expired = claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC);
      return expired.isBefore(OffsetDateTime.now());
    } catch (ApiException ex) {
      return false;
    }
  }
}