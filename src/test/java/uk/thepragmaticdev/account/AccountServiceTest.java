package uk.thepragmaticdev.account;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
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
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AccountCode;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.log.billing.BillingLog;
import uk.thepragmaticdev.log.billing.BillingLogService;
import uk.thepragmaticdev.log.security.SecurityLog;
import uk.thepragmaticdev.log.security.SecurityLogService;
import uk.thepragmaticdev.security.JwtTokenProvider;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@SpringBootTest
public class AccountServiceTest extends UnitData {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private BillingLogService billingLogService;

  @Mock
  private SecurityLogService securityLogService;

  @Mock
  private EmailService emailService;

  @Mock
  private RequestMetadataService requestMetadataService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private StatefulBeanToCsv<BillingLog> billingLogWriter;

  @Mock
  private StatefulBeanToCsv<SecurityLog> securityLogWriter;

  private AccountService sut;

  /**
   * Called before each test, builds the system under test.
   */
  @BeforeEach
  public void initEach() {
    sut = new AccountService(accountRepository, billingLogService, securityLogService, emailService,
        requestMetadataService, passwordEncoder, jwtTokenProvider, authenticationManager);
  }

  @Test
  public void shouldThrowExceptionIfTokenHasExpired() {
    var account = account();
    account.setPasswordResetTokenExpire(OffsetDateTime.now().minusMinutes(1));
    when(accountRepository.findByPasswordResetToken(anyString())).thenReturn(Optional.of(account));

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.reset("password", "token");
    });
    assertThat(ex.getErrorCode(), is(AccountCode.INVALID_PASSWORD_RESET_TOKEN));
  }

  @Test
  public void shouldThrowExceptionDownloadingBillingLogsIfInvalidCsvDataType() throws Exception {
    when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(account()));
    doThrow(CsvDataTypeMismatchException.class).when(billingLogWriter).write(anyList());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.downloadBillingLogs(billingLogWriter, "username");
    });
    assertThat(ex.getErrorCode(), is(CriticalCode.CSV_WRITING_ERROR));
  }

  @Test
  public void shouldThrowExceptionDownloadingBillingLogsIfRequiredFieldIsEmpty() throws Exception {
    when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(account()));
    doThrow(CsvRequiredFieldEmptyException.class).when(billingLogWriter).write(anyList());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.downloadBillingLogs(billingLogWriter, "username");
    });
    assertThat(ex.getErrorCode(), is(CriticalCode.CSV_WRITING_ERROR));
  }

  @Test
  public void shouldThrowExceptionDownloadingSecurityLogsIfInvalidCsvDataType() throws Exception {
    when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(account()));
    doThrow(CsvDataTypeMismatchException.class).when(securityLogWriter).write(anyList());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.downloadSecurityLogs(securityLogWriter, "username");
    });
    assertThat(ex.getErrorCode(), is(CriticalCode.CSV_WRITING_ERROR));
  }

  @Test
  public void shouldThrowExceptionDownloadingSecurityLogsIfRequiredFieldIsEmpty() throws Exception {
    when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(account()));
    doThrow(CsvRequiredFieldEmptyException.class).when(securityLogWriter).write(anyList());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.downloadSecurityLogs(securityLogWriter, "username");
    });
    assertThat(ex.getErrorCode(), is(CriticalCode.CSV_WRITING_ERROR));
  }
}