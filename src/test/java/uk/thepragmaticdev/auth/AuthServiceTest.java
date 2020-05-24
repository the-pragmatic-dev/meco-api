package uk.thepragmaticdev.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.thepragmaticdev.UnitData;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AccountCode;
import uk.thepragmaticdev.log.security.SecurityLogService;
import uk.thepragmaticdev.security.request.RequestMetadataService;
import uk.thepragmaticdev.security.token.TokenService;

@SpringBootTest
class AuthServiceTest extends UnitData {

  @Mock
  private AccountService accountService;

  @Mock
  private SecurityLogService securityLogService;

  @Mock
  private EmailService emailService;

  @Mock
  private RequestMetadataService requestMetadataService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private TokenService tokenService;

  @Mock
  private AuthenticationManager authenticationManager;

  private AuthService sut;

  /**
   * Called before each test, builds the system under test.
   */
  @BeforeEach
  public void initEach() {
    sut = new AuthService(accountService, securityLogService, emailService, requestMetadataService, passwordEncoder,
        tokenService, authenticationManager);
  }

  @Test
  void shouldThrowExceptionIfTokenHasExpired() {
    var account = account();
    account.setPasswordResetTokenExpire(OffsetDateTime.now().minusMinutes(1));
    when(accountService.findByPasswordResetToken(anyString())).thenReturn(Optional.of(account));

    // TODO BROKEN
    // var ex = Assertions.assertThrows(ApiException.class, () -> {
    // sut.reset("password", "token");
    // });
    // assertThat(ex.getErrorCode(), is(AccountCode.INVALID_PASSWORD_RESET_TOKEN));
  }
}