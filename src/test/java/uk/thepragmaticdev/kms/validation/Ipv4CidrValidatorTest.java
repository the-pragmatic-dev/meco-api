package uk.thepragmaticdev.kms.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.List;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import uk.thepragmaticdev.kms.dto.request.AccessPolicyRequest;

@SpringBootTest
public class Ipv4CidrValidatorTest {

  private Ipv4CidrValidator sut;

  @BeforeEach
  public void initEach() {
    sut = new Ipv4CidrValidator();
  }

  @ParameterizedTest
  @MethodSource("validAccessPolicyProvider")
  public void shouldReturnIsValid(List<AccessPolicyRequest> accessPolicy) {
    var ret = sut.isValid(accessPolicy, mock(ConstraintValidatorContext.class));
    assertThat(ret, is(true));
  }

  @Test
  public void shouldReturnIsValidIfNullPolicies() {
    var ret = sut.isValid(null, mock(ConstraintValidatorContext.class));
    assertThat(ret, is(true));
  }

  @ParameterizedTest
  @MethodSource("invalidAccessPolicyProvider")
  public void shouldReturnInvalid(List<AccessPolicyRequest> accessPolicy) {
    var ret = sut.isValid(accessPolicy, mock(ConstraintValidatorContext.class));
    assertThat(ret, is(false));
  }

  private static List<List<AccessPolicyRequest>> validAccessPolicyProvider() {
    List<List<AccessPolicyRequest>> validAccessPolicyProvider = List.of(//
        List.of(), // empty list is valid
        List.of(// one valid
            new AccessPolicyRequest("name1", "0.0.0.0/24")//
        ), //
        List.of(// muliple valid
            new AccessPolicyRequest("name1", "0.0.0.0/0"), //
            new AccessPolicyRequest("name1", "0.0.0.0/32"), //
            new AccessPolicyRequest("name1", "255.0.0.0/32"), //
            new AccessPolicyRequest("name1", "0.255.0.0/32"), //
            new AccessPolicyRequest("name1", "0.0.255.0/32"), //
            new AccessPolicyRequest("name1", "0.0.0.255/32")//
        )//
    );
    return validAccessPolicyProvider;
  }

  private static List<List<AccessPolicyRequest>> invalidAccessPolicyProvider() {
    List<List<AccessPolicyRequest>> invalidAccessPolicyProvider = List.of(//
        List.of(// missing prefix number
            new AccessPolicyRequest("name1", "0.0.0.0/")//
        ), //
        List.of(// missing prefix slash
            new AccessPolicyRequest("name1", "0.0.0.0")//
        ), //
        List.of(// large prefix
            new AccessPolicyRequest("name1", "0.0.0.0/33")//
        ), //
        List.of(// large group one
            new AccessPolicyRequest("name1", "256.0.0.0/24")//
        ), //
        List.of(// large group two
            new AccessPolicyRequest("name1", "0.256.0.0/24")//
        ), //
        List.of(// large group three
            new AccessPolicyRequest("name1", "0.0.256.0/24")//
        ), //
        List.of(// large group four
            new AccessPolicyRequest("name1", "0.0.0.256/24")//
        ), //
        List.of(// one valid and one invalid
            new AccessPolicyRequest("name1", "0.0.0.0/0"), //
            new AccessPolicyRequest("name1", "0.0.0.256/24")//
        )//
    );
    return invalidAccessPolicyProvider;
  }
}