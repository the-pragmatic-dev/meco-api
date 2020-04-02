package uk.thepragmaticdev.kms.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = Ipv4CidrValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ipv4Cidr {

  /**
   * TODO.
   * 
   * @return
   */
  String message() default "Must match n.n.n.n/m where n=1-3 decimal digits, m = 1-3 decimal digits in range 1-32.";

  /**
   * TODO.
   * 
   * @return
   */
  Class<?>[] groups() default {};

  /**
   * TODO.
   * 
   * @return
   */
  Class<? extends Payload>[] payload() default {};
}