package uk.thepragmaticdev.kms.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.List;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.thepragmaticdev.kms.AccessPolicy;

public class Ipv4CidrValidatorTest {

  private Ipv4CidrValidator sut;

  @BeforeEach
  public void initEach() {
    sut = new Ipv4CidrValidator();
  }

  @ParameterizedTest
  @MethodSource("validAccessPolicyProvider")
  public void shouldReturnIsValid(List<AccessPolicy> accessPolicy) {
    var ret = sut.isValid(accessPolicy, mock(ConstraintValidatorContext.class));
    assertThat(ret, is(true));
  }

  @ParameterizedTest
  @MethodSource("invalidAccessPolicyProvider")
  public void shouldReturnInvalid(List<AccessPolicy> accessPolicy) {
    var ret = sut.isValid(accessPolicy, mock(ConstraintValidatorContext.class));
    assertThat(ret, is(false));
  }

  private static List<List<AccessPolicy>> validAccessPolicyProvider() {
    List<List<AccessPolicy>> validAccessPolicyProvider = List.of(//
        List.of(), // empty list is valid
        List.of(// one valid
            new AccessPolicy(1L, "name1", "0.0.0.0/24", null)//
        ), //
        List.of(// muliple valid
            new AccessPolicy(1L, "name1", "0.0.0.0/0", null), //
            new AccessPolicy(1L, "name1", "0.0.0.0/32", null), //
            new AccessPolicy(1L, "name1", "255.0.0.0/32", null), //
            new AccessPolicy(1L, "name1", "0.255.0.0/32", null), //
            new AccessPolicy(1L, "name1", "0.0.255.0/32", null), //
            new AccessPolicy(1L, "name1", "0.0.0.255/32", null)//
        )//
    );
    return validAccessPolicyProvider;
  }

  private static List<List<AccessPolicy>> invalidAccessPolicyProvider() {
    List<List<AccessPolicy>> invalidAccessPolicyProvider = List.of(//
        List.of(// missing prefix number
            new AccessPolicy(1L, "name1", "0.0.0.0/", null)//
        ), //
        List.of(// missing prefix slash
            new AccessPolicy(1L, "name1", "0.0.0.0", null)//
        ), //
        List.of(// large prefix
            new AccessPolicy(1L, "name1", "0.0.0.0/33", null)//
        ), //
        List.of(// large group one
            new AccessPolicy(1L, "name1", "256.0.0.0/24", null)//
        ), //
        List.of(// large group two
            new AccessPolicy(1L, "name1", "0.256.0.0/24", null)//
        ), //
        List.of(// large group three
            new AccessPolicy(1L, "name1", "0.0.256.0/24", null)//
        ), //
        List.of(// large group four
            new AccessPolicy(1L, "name1", "0.0.0.256/24", null)//
        ), //
        List.of(// one valid and one invalid
            new AccessPolicy(1L, "name1", "0.0.0.0/0", null), //
            new AccessPolicy(1L, "name1", "0.0.0.256/24", null)//
        )//
    );
    return invalidAccessPolicyProvider;
  }
}