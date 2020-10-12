package uk.thepragmaticdev.kms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.ApiKeyCode;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.kms.scope.Scope;
import uk.thepragmaticdev.log.key.ApiKeyLog;
import uk.thepragmaticdev.log.key.ApiKeyLogService;
import uk.thepragmaticdev.log.security.SecurityLogService;

@SpringBootTest
class ApiKeyServiceTest {

  @Mock
  private AccountService accountService;

  @Mock
  private ApiKeyRepository apiKeyRepository;

  @Mock
  private ApiKeyLogService apiKeyLogService;

  @Mock
  private SecurityLogService securityLogService;

  @Mock
  private EmailService emailService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private StatefulBeanToCsv<ApiKeyLog> apiKeyLogWriter;

  private ApiKeyService sut;

  /**
   * Called before each test, builds the system under test.
   */
  @BeforeEach
  public void initEach() {
    // defaulting key allowance to 10
    sut = new ApiKeyService(accountService, apiKeyRepository, apiKeyLogService, securityLogService, emailService,
        passwordEncoder, 10);
  }

  @Test
  void shouldCreateValidKey() {
    var keyName = "testKey";
    var encodedString = "anEncodedString";
    var apiKey = new ApiKey();
    apiKey.setName(keyName);
    apiKey.setScope(new Scope());

    when(passwordEncoder.encode(anyString())).thenReturn(encodedString);
    when(apiKeyRepository.countByAccountIdAndDeletedDateIsNull(anyLong())).thenReturn(0L);
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(mock(Account.class));
    when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

    var ret = sut.create("username", apiKey);
    assertThat(ret.getName(), is(keyName));
    assertThat(ret.getPrefix().length(), is(7));
    assertThat(ret.getHash(), is(encodedString));
    assertThat(ret.getKey(), startsWith(ret.getPrefix().concat(".")));
    assertThat(StringUtils.countMatches(ret.getKey(), "."), is(1));
    assertThat(ret.getKey().substring(ret.getKey().lastIndexOf(".") + 1).length(), is(48));
  }

  @Test()
  void shouldThrowExceptionWhenMaxKeysReached() {
    var apiKey = new ApiKey();
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(mock(Account.class));
    when(apiKeyRepository.countByAccountIdAndDeletedDateIsNull(anyLong())).thenReturn(10L); // max keys

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.create("username", apiKey);
    });
    assertThat(ex.getErrorCode(), is(ApiKeyCode.API_KEY_LIMIT));
  }

  @Test
  void shouldThrowExceptionDownloadingApiKeyLogsIfInvalidCsvDataType() throws Exception {
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(mock(Account.class));
    when(apiKeyRepository.findOneByIdAndAccountIdAndDeletedDateIsNull(anyLong(), anyLong()))
        .thenReturn(Optional.of(mock(ApiKey.class)));
    doThrow(CsvDataTypeMismatchException.class).when(apiKeyLogWriter).write(anyList());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.downloadLog(apiKeyLogWriter, "username", 1);
    });
    assertThat(ex.getErrorCode(), is(CriticalCode.CSV_WRITING_ERROR));
  }

  @Test
  void shouldThrowExceptionDownloadingApiKeyLogsIfRequiredFieldIsEmpty() throws Exception {
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(mock(Account.class));
    when(apiKeyRepository.findOneByIdAndAccountIdAndDeletedDateIsNull(anyLong(), anyLong()))
        .thenReturn(Optional.of(mock(ApiKey.class)));
    doThrow(CsvRequiredFieldEmptyException.class).when(apiKeyLogWriter).write(anyList());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.downloadLog(apiKeyLogWriter, "username", 1);
    });
    assertThat(ex.getErrorCode(), is(CriticalCode.CSV_WRITING_ERROR));
  }

  @Test
  void shouldReturnTrueWhenRawKeyMatchesHashedKey() {
    var passwordEncoderImpl = new BCryptPasswordEncoder(12);
    var rawKey = "abcdefg.hijklmnopqrstuvwxyz";
    var hashedKey = passwordEncoderImpl.encode(rawKey);
    when(passwordEncoder.matches(rawKey, hashedKey)).thenReturn(passwordEncoderImpl.matches(rawKey, hashedKey));
    assertThat(sut.isAuthentic(rawKey, hashedKey), is(true));
  }

  @Test
  void shouldReturnFalseWhenRawKeyDoesNotMatchHashedKey() {
    var passwordEncoderImpl = new BCryptPasswordEncoder(12);
    var rawKey = "abcdefg.hijklmnopqrstuvwxyz";
    var hashedKey = "aSuperSecretHashThatWillNotMatchTheRawKeyWhenHashed";
    when(passwordEncoder.matches(rawKey, hashedKey)).thenReturn(passwordEncoderImpl.matches(rawKey, hashedKey));
    assertThat(sut.isAuthentic(rawKey, hashedKey), is(false));
  }

  @Test
  void shouldExtractRawKey() {
    var apiKey = "abcdefg.hijklmnopqrstuvwxyz";
    var request = mock(HttpServletRequest.class);
    when(request.getHeader(anyString())).thenReturn("ApiKey " + apiKey);

    assertThat(sut.extract(request), is(apiKey));
  }

  @Test
  void shouldNotExtractRawKeyIfHeaderIsMissing() {
    var request = mock(HttpServletRequest.class);
    when(request.getHeader(anyString())).thenReturn(null);

    assertThat(sut.extract(request), is(nullValue()));
  }

  @Test
  void shouldNotExtractRawKeyIfHeaderPrefixIsMissing() {
    var apiKey = "abcdefg.hijklmnopqrstuvwxyz";
    var request = mock(HttpServletRequest.class);
    when(request.getHeader(anyString())).thenReturn(apiKey);

    assertThat(sut.extract(request), is(nullValue()));
  }

  @Test
  void shouldNotExtractRawKeyIfHeaderPrefixIsInvalid() {
    var apiKey = "abcdefg.hijklmnopqrstuvwxyz";
    var request = mock(HttpServletRequest.class);
    when(request.getHeader(anyString())).thenReturn("InvalidPrefix " + apiKey);

    assertThat(sut.extract(request), is(nullValue()));
  }
}