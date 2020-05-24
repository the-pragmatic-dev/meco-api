package uk.thepragmaticdev.auth;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.thepragmaticdev.UnitData;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.email.EmailService;
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
}