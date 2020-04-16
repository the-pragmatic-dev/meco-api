package uk.thepragmaticdev.kms.validation;

import java.util.Collection;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.net.util.SubnetUtils;
import uk.thepragmaticdev.kms.AccessPolicy;

public class Ipv4CidrValidator implements ConstraintValidator<Ipv4Cidr, Collection<AccessPolicy>> {

  @Override
  public boolean isValid(Collection<AccessPolicy> values, ConstraintValidatorContext context) {
    // Return valid if no access policies exist
    if (values == null) {
      return true;
    }
    for (AccessPolicy accessPolicy : values) {
      try {
        new SubnetUtils(accessPolicy.getRange());
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
    return true;
  }
}