package uk.thepragmaticdev.billing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PlanCollection;
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
  private BillingRepository billingRepository;

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
    var expireGracePeriod = 3;
    sut = new BillingService(billingRepository, accountService, stripeService, expireGracePeriod);
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenFindingAllPlans() throws Exception {
    doThrow(ApiConnectionException.class).when(stripeService).findAllPlans(anyBoolean());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.findAllPlans();
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_FIND_ALL_PLANS_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenCreatingCustomer() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getCustomerId()).thenReturn(null);
    when(account.getBilling()).thenReturn(billing);
    when(account.getUsername()).thenReturn("user@name.com");
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);

    doThrow(ApiConnectionException.class).when(stripeService).createCustomer(anyString(), anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.createCustomer("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_CREATE_CUSTOMER_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeConflictWhenCreatingCustomer() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getCustomerId()).thenReturn("existingcustomerid");
    when(account.getBilling()).thenReturn(billing);
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.createCustomer("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_CREATE_CUSTOMER_CONFLICT));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenDeletingCustomer() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getCustomerId()).thenReturn("id");
    when(account.getBilling()).thenReturn(billing);
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
    var planCollection = mock(PlanCollection.class);

    var billing = mock(Billing.class);
    when(billing.getCustomerId()).thenReturn("id");
    when(account.getBilling()).thenReturn(billing);

    var paymentMethod = mock(PaymentMethod.class);
    when(paymentMethod.getId()).thenReturn("id");

    when(planCollection.getData()).thenReturn(List.of(plan()));
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    when(stripeService.findAllPlans(anyBoolean())).thenReturn(planCollection);
    when(stripeService.attachPaymentMethod(anyString(), anyString())).thenReturn(paymentMethod);
    doThrow(ApiConnectionException.class).when(stripeService).createSubscription(anyString(), anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.createSubscription("username", "paymentMethodId", "planId");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_CREATE_SUBSCRIPTION_ERROR));
  }

  @Test
  void shouldThrowExceptioWhenPlanNotFound() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getCustomerId()).thenReturn("id");
    when(account.getBilling()).thenReturn(billing);

    var planCollection = mock(PlanCollection.class);
    when(planCollection.getData()).thenReturn(List.of(plan()));
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    when(stripeService.findAllPlans(anyBoolean())).thenReturn(planCollection);

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.createSubscription("username", "paymentMethodId", "invalidPlanId");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_PLAN_NOT_FOUND));
  }

  @Test
  void shouldThrowExceptionWhenSubscriptionNotFound() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getCustomerId()).thenReturn("id");
    when(billing.getSubscriptionId()).thenReturn(null);
    when(account.getBilling()).thenReturn(billing);

    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).createSubscription(anyString(), anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.cancelSubscription("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_SUBSCRIPTION_NOT_FOUND));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenCreatingUsageRecord() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getSubscriptionItemId()).thenReturn("id");
    when(account.getBilling()).thenReturn(billing);
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).createUsageRecord(anyString(), anyLong(), anyLong());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      var operations = 1;
      sut.createUsageRecord("username", operations);
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_USAGE_RECORD_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenFindingAllUsageRecords() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getSubscriptionItemId()).thenReturn("id");
    when(account.getBilling()).thenReturn(billing);
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    doThrow(ApiConnectionException.class).when(stripeService).findAllUsageRecords(anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.findAllUsageRecords("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_FIND_ALL_USAGE_RECORDS_ERROR));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenFindingUpcomingInvoiceNotFound() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getCustomerId()).thenReturn("id");
    when(account.getBilling()).thenReturn(billing);

    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    var error = mock(InvalidRequestException.class);
    when(error.getCode()).thenReturn("invoice_upcoming_none");
    doThrow(error).when(stripeService).findUpcomingInvoice(anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.findUpcomingInvoice("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_NOT_FOUND));
  }

  @Test
  void shouldThrowExceptionOnStripeErrorWhenFindingUpcomingInvoiceError() throws Exception {
    var account = mock(Account.class);
    var billing = mock(Billing.class);
    when(billing.getCustomerId()).thenReturn("id");
    when(account.getBilling()).thenReturn(billing);

    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);
    var error = mock(InvalidRequestException.class);
    when(error.getCode()).thenReturn("code");
    doThrow(error).when(stripeService).findUpcomingInvoice(anyString());

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.findUpcomingInvoice("username");
    });
    assertThat(ex.getErrorCode(), is(BillingCode.STRIPE_FIND_UPCOMING_INVOICE_ERROR));
  }
}