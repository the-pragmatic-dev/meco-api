package uk.thepragmaticdev.security.key;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationTokenTest {

  private ApiKeyAuthenticationToken sut;

  @BeforeEach
  public void initEach() throws IOException {
    sut = new ApiKeyAuthenticationToken(null);
  }

  @Test
  void shouldReturnEmptyCredentials() {
    assertThat(sut.getCredentials(), is(""));
  }

  @Test()
  void shouldBeAuthenticatedByDefault() {
    assertThat(sut.isAuthenticated(), is(true));
  }

  @Test()
  void shouldNotThrowExceptionWhenManuallyTryingToUnauthenticate() {
    sut.setAuthenticated(false);
    assertThat(sut.isAuthenticated(), is(false));
  }

  @Test()
  void shouldThrowExceptionWhenManuallyTryingToAuthenticate() {
    var ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      sut.setAuthenticated(true);
    });
    assertThat(ex.getMessage(), is("Cannot set this token to trusted - use constructor instead."));
  }

  @Test()
  void shouldNullCredentialsWhenErased() {
    sut.eraseCredentials();
    assertThat(sut.getCredentials(), is(nullValue()));
  }
}
