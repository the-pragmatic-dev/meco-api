package uk.thepragmaticdev.auth.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = ExpiredAccessTokenValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExpiredAccessToken {

  /**
   * Constraint to check that a given access token is valid and has expired.
   * 
   * @return The error message if validation fails
   */
  String message() default "Must be a valid, expired access token.";

  /**
   * Specifies the processing groups with which the constraint declaration is
   * associated.
   * 
   * @return The default empty array value
   */
  Class<?>[] groups() default {};

  /**
   * The payload with which the constraint declaration is associated.
   * 
   * @return The default empty array value
   */
  Class<? extends Payload>[] payload() default {};
}