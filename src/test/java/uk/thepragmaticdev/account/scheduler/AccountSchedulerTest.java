package uk.thepragmaticdev.account.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import uk.thepragmaticdev.UnitData;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.billing.Billing;
import uk.thepragmaticdev.kms.usage.ApiKeyUsageService;

@SpringBootTest
class AccountSchedulerTest extends UnitData {

  @Mock
  private AccountService accountService;

  @Mock
  private ApiKeyUsageService apiKeyUsageService;

  private AccountScheduler sut;

  /**
   * Called before each test, builds the system under test.
   */
  @BeforeEach
  public void initEach() {
    var freePlanLimit = 10;
    sut = new AccountScheduler(accountService, apiKeyUsageService, freePlanLimit);
  }

  @Test
  void shouldNotCallFreezeIfNoAccountsExist() throws Exception {
    when(accountService.findAll()).thenReturn(List.of());
    sut.freeze();
    verify(accountService, times(0)).freeze(any());
  }

  @Test
  void shouldCallFreezeIfAccountHasNoPlan() throws Exception {
    var account = mock(Account.class);
    when(account.getBilling()).thenReturn(mock(Billing.class));
    when(accountService.findAll()).thenReturn(List.of(account));

    sut.freeze();

    verify(accountService, times(1)).freeze(account);
  }

  @Test
  void shouldCallFreezeIfSubscriptionPeriodHasEnded() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getPlanNickname()).thenReturn("some_plan_nickname");
    when(billing.getSubscriptionCurrentPeriodEnd()).thenReturn(OffsetDateTime.now().minusHours(1));
    when(account.getBilling()).thenReturn(billing);
    when(accountService.findAll()).thenReturn(List.of(account));

    sut.freeze();

    verify(accountService, times(1)).freeze(account);
  }

  @Test
  void shouldCallFreezeIfOnStarterPlanAndExceededLimit() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getPlanNickname()).thenReturn("starter");
    when(billing.getSubscriptionCurrentPeriodStart()).thenReturn(OffsetDateTime.now().minusHours(1));
    when(billing.getSubscriptionCurrentPeriodEnd()).thenReturn(OffsetDateTime.now().plusHours(1));
    when(account.getBilling()).thenReturn(billing);
    when(accountService.findAll()).thenReturn(List.of(account));
    when(apiKeyUsageService.count(any())).thenReturn(10L);

    sut.freeze();

    verify(accountService, times(1)).freeze(account);
  }

  @Test
  void shouldNotCallFreezeIfOnStarterPlanAndNotExceededLimit() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getPlanNickname()).thenReturn("starter");
    when(billing.getSubscriptionCurrentPeriodStart()).thenReturn(OffsetDateTime.now().minusHours(1));
    when(billing.getSubscriptionCurrentPeriodEnd()).thenReturn(OffsetDateTime.now().plusHours(1));
    when(account.getBilling()).thenReturn(billing);
    when(accountService.findAll()).thenReturn(List.of(account));
    when(apiKeyUsageService.count(any())).thenReturn(9L);

    sut.freeze();

    verify(accountService, times(0)).freeze(account);
  }
}