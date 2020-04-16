package uk.thepragmaticdev.kms.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;

@Constraint(validatedBy = Ipv4CidrValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ipv4Cidr {

  /**
   * Constraint to check pattern of ipv4 cidr blocks.
   * 
   * @return The error message if validation fails
   */
  String message() default "Must match n.n.n.n/m where n=1-3 decimal digits, m = 1-3 decimal digits in range 1-32.";
}