package uk.thepragmaticdev.security.token;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AuthCode;
import uk.thepragmaticdev.security.UserService;

@SpringBootTest
class TokenServiceTest {

  @Mock
  private UserService userService;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Value("${security.token.secret-key}")
  private String secretKey;

  private TokenService sut;

  /**
   * Called before each test, builds the system under test.
   */
  @BeforeEach
  public void initEach() {
    var accessTokenExpiration = 1;
    var refreshTokenExpiration = 2;
    sut = new TokenService(userService, refreshTokenRepository, secretKey, accessTokenExpiration,
        refreshTokenExpiration);
  }

  @Test
  void shouldReturnTokenFromBearer() {
    var bearerToken = "Bearer test";
    var request = mock(HttpServletRequest.class);
    when(request.getHeader("Authorization")).thenReturn(bearerToken);

    var ret = sut.resolveToken(request);
    assertThat(ret, is("test"));
  }

  @Test
  void shouldReturnNullIfBearerIsNull() {
    var request = mock(HttpServletRequest.class);
    when(request.getHeader("Authorization")).thenReturn(null);

    var ret = sut.resolveToken(request);
    assertThat(ret, is(nullValue()));
  }

  @Test
  void shouldReturnNullIfBearerPrefixHasNoSpace() {
    var bearerToken = "Bearer";
    var request = mock(HttpServletRequest.class);
    when(request.getHeader("Authorization")).thenReturn(bearerToken);

    var ret = sut.resolveToken(request);
    assertThat(ret, is(nullValue()));
  }

  @Test
  void shouldReturnValidForNonExpiredToken() {
    var token = futureToken();

    var ret = sut.validateToken(token);
    assertThat(ret, is(true));
  }

  @Test
  void shouldReturnNotValidForNullToken() {
    var ret = sut.validateToken(null);
    assertThat(ret, is(false));
  }

  @Test
  void shouldThrowJwtExceptionForExpiredToken() {
    var token = pastToken();

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.validateToken(token);
    });
    assertThat(ex.getErrorCode(), is(AuthCode.INVALID_EXPIRED_TOKEN));
  }

  private String futureToken() {
    // generated with an expiration date of 30th September 3921
    return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBlbWFpbC5jb20iLCJhdXRoIjpbeyJhdX"
        + "Rob3JpdHkiOiJST0xFX0FETUlOIn1dLCJpYXQiOjE1OTAzMTcxNTMsImV4cCI6NjE1OTAzMT"
        + "cwOTN9.DX6oWfZ1tU3l7gssicEQfcEzyXQqphNyxwBnVcnSsaI";
  }

  private String pastToken() {
    // generated with an expiration date of 24th May 2020
    return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBlbWFpbC5jb20iLCJhdXRoIjpbeyJhdX"
        + "Rob3JpdHkiOiJST0xFX0FETUlOIn1dLCJpYXQiOjE1OTAzMTQ5MjQsImV4cCI6MTU5MDMxNT"
        + "IyNH0.dPoby2Rew0Imq3giTk-K2uI_BG35HqkWJn43HU2iaIk";
  }
}