package uk.thepragmaticdev.auth.validation;

import java.util.UUID;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class RefreshTokenValidator implements ConstraintValidator<RefreshToken, String> {

  @Override
  public boolean isValid(String refreshToken, ConstraintValidatorContext context) {
    // Return invalid if no refresh token exists
    if (StringUtils.isBlank(refreshToken)) {
      return false;
    }
    try {
      UUID.fromString(refreshToken);
    } catch (IllegalArgumentException ex) {
      return false;
    }
    return true;
  }
}