package uk.thepragmaticdev.kms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.ApiKeyCode;
import uk.thepragmaticdev.log.key.ApiKeyLogService;

@SpringBootTest
public class ApiKeyServiceTest {

  @Mock
  private AccountService accountService;

  @Mock
  private ApiKeyRepository apiKeyRepository;

  @Mock
  private ApiKeyLogService apiKeyLogService;

  @Mock
  private EmailService emailService;

  @Mock
  private PasswordEncoder passwordEncoder;

  private ApiKeyService sut;

  @BeforeEach
  public void initEach() {
    // defaulting key allowance to 10
    sut = new ApiKeyService(accountService, apiKeyRepository, apiKeyLogService, emailService, passwordEncoder, 10);
  }

  @Test
  public void shouldCreateValidKey() {
    var keyName = "testKey";
    var encodedString = "anEncodedString";
    var apiKey = new ApiKey();
    apiKey.setName(keyName);

    when(passwordEncoder.encode(anyString())).thenReturn(encodedString);
    when(apiKeyRepository.countByAccountId(anyLong())).thenReturn(0L);
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
  public void shouldThrowExceptionWhenMaxKeysReached() {
    var apiKey = new ApiKey();
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(mock(Account.class));
    when(apiKeyRepository.countByAccountId(anyLong())).thenReturn(10L); // max keys

    ApiException ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.create("username", apiKey);
    });
    assertThat(ex.getErrorCode(), is(ApiKeyCode.API_KEY_LIMIT));
  }
}