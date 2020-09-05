package uk.thepragmaticdev.billing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stripe.exception.ApiConnectionException;
import com.stripe.model.PriceCollection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import uk.thepragmaticdev.UnitData;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.BillingCode;

@SpringBootTest
class BillingServiceTest extends UnitData {

  @Mock
  private AccountService accountService;

  @Mock
  private StripeService stripeService;

  private BillingService sut;

  /**
   * Called before each test, builds the system under test.
   */
  @BeforeEach
  public void initEach() {
    sut = new BillingService(accountService, stripeService);
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenFindingAllPrices() throws Exception {
    doThrow(ApiConnectionException.class).when(stripeService).findAllPrices(anyMap());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.findAllPrices();
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_FIND_ALL_PRICES_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenCreatingCustomer() throws Exception {
    var account = mock(Account.class);
    when(account.getStripeCustomerId()).thenReturn(null);
    when(account.getUsername()).thenReturn("user@name.com");
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);

    doThrow(ApiConnectionException.class).when(stripeService).createCustomer(anyMap());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.createCustomer("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_CREATE_CUSTOMER_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeConflictWhenCreatingCustomer() throws Exception {
    var account = mock(Account.class);
    when(account.getStripeCustomerId()).thenReturn("existingcustomerid");
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.createCustomer("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_CREATE_CUSTOMER_CONFLICT));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenDeletingCustomer() throws Exception {
    var account = mock(Account.class);
    when(account.getStripeCustomerId()).thenReturn("id");
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).deleteCustomer(anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.deleteCustomer("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_DELETE_CUSTOMER_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenCreatingSubscription() throws Exception {
    var account = mock(Account.class);
    var priceCollection = mock(PriceCollection.class);
    when(account.getStripeCustomerId()).thenReturn("id");
    when(priceCollection.getData()).thenReturn(List.of(price()));
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    when(stripeService.findAllPrices(anyMap())).thenReturn(priceCollection);
    doThrow(ApiConnectionException.class).when(stripeService).createSubscription(anyMap());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.createSubscription("username", "priceId");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_CREATE_SUBSCRIPTION_ERROR));
  }

  @Test
  void shouldThrowExceptioWhenPriceNotFound() throws Exception {
    var account = mock(Account.class);
    var priceCollection = mock(PriceCollection.class);
    when(priceCollection.getData()).thenReturn(List.of(price()));
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    when(stripeService.findAllPrices(anyMap())).thenReturn(priceCollection);

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.createSubscription("username", "invalidPriceId");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_PRICE_NOT_FOUND));
  }

  @Test
  void shouldThrowExceptionWhenSubscriptionNotFound() throws Exception {
    var account = mock(Account.class);
    when(account.getStripeSubscriptionId()).thenReturn(null);
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).createSubscription(anyMap());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.cancelSubscription("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenCancellingSubscription() throws Exception {
    var account = mock(Account.class);
    when(account.getStripeSubscriptionId()).thenReturn("id");
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).cancelSubscription(anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.cancelSubscription("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_CANCEL_SUBSCRIPTION_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenCreatingUsageRecord() throws Exception {
    var account = mock(Account.class);
    when(account.getStripeSubscriptionItemId()).thenReturn("id");
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).createUsageRecord(anyString(), anyMap());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      var operations = 1;
      sut.createUsageRecord("username", operations);
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_USAGE_RECORD_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenFindingAllUsageRecords() throws Exception {
    var account = mock(Account.class);
    when(account.getStripeSubscriptionItemId()).thenReturn("id");
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).findAllUsageRecords(anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.findAllUsageRecords("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_FIND_ALL_USAGE_RECORDS_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenFindingUpcomingInvoice() throws Exception {
    var account = mock(Account.class);
    when(account.getStripeCustomerId()).thenReturn("id");
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).findUpcomingInvoice(anyMap());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.findUpcomingInvoice("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_ERROR));
  }
}