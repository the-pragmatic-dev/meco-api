package uk.thepragmaticdev.kms.validation;

import java.util.Collection;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.net.util.SubnetUtils;
import uk.thepragmaticdev.kms.dto.request.AccessPolicyRequest;

public class Ipv4CidrValidator implements ConstraintValidator<Ipv4Cidr, Collection<AccessPolicyRequest>> {

  @Override
  public boolean isValid(Collection<AccessPolicyRequest> values, ConstraintValidatorContext context) {
    // Return valid if no access policies exist
    if (values == null) {
      return true;
    }
    for (var accessPolicy : values) {
      try {
        if (accessPolicy.getRange() == null) {
          throw new IllegalArgumentException();
        }
        new SubnetUtils(accessPolicy.getRange());
      } catch (IllegalArgumentException ex) {
        return false;
      }
    }
    return true;
  }
}