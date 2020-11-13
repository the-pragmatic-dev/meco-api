package uk.thepragmaticdev.log.billing;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.account.Account;

@Service
public class BillingLogService {

  private final BillingLogRepository billingLogRepository;

  /**
   * Service for logging billing events such as invoice creation.
   * 
   * @param billingLogRepository The data access repository for billing logs
   */
  @Autowired
  public BillingLogService(BillingLogRepository billingLogRepository) {
    this.billingLogRepository = billingLogRepository;
  }

  /**
   * Find all logs for the requested account.
   * 
   * @param account The authenticated account
   * @return A list of all logs for the requested account
   */
  public List<BillingLog> findAllByAccountId(Account account) {
    return billingLogRepository.findAllByAccountIdOrderByCreatedDateDesc(account.getId());
  }

  /**
   * Find the latest logs for the requested account.
   * 
   * @param pageable The pagination information
   * @param account  The authenticated account
   * @return
   */
  public Page<BillingLog> findAllByAccountId(Pageable pageable, Account account) {
    return billingLogRepository.findAllByAccountIdOrderByCreatedDateDesc(pageable, account.getId());
  }

  /**
   * Log a created customer event.
   * 
   * @param account The account containing billing information.
   * @return The persisted log
   */
  public BillingLog createCustomer(Account account) {
    return log(account, "billing.customer.created");
  }

  /**
   * Log a created subscription event.
   * 
   * @param account  The account containing billing information.
   * @param nickname The created subscription nickname
   * @return The persisted log
   */
  public BillingLog createSubscription(Account account, String nickname) {
    return log(account, String.format("billing.subscription.%s.created", nickname));
  }

  /**
   * Log an updated subscription event.
   * 
   * @param account          The account containing billing information.
   * @param existingNickname The old subscription nickname
   * @param newNickname      The new subscription nickname
   * @return The persisted log
   */
  public BillingLog updateSubscription(Account account, String existingNickname, String newNickname) {
    deleteSubscription(account, existingNickname);
    return createSubscription(account, newNickname);
  }

  /**
   * Log a deleted subscription event.
   * 
   * @param account  The account containing billing information.
   * @param nickname The deleted subscription nickname
   * @return The persisted log
   */
  public BillingLog deleteSubscription(Account account, String nickname) {
    return log(account, String.format("billing.subscription.%s.deleted", nickname));
  }

  /**
   * Log an attached payment method event.
   * 
   * @param account The account containing billing information.
   * @return The persisted log
   */
  public BillingLog attachPaymentMethod(Account account, String last4) {
    return log(account, String.format("billing.payment_method.%s.attached", last4));
  }

  /**
   * Log a set default payment method event.
   * 
   * @param account The account containing billing information.
   * @return The persisted log
   */
  public BillingLog defaultPaymentMethod(Account account, String last4) {
    return log(account, String.format("billing.payment_method.%s.default_set", last4));
  }

  /**
   * Log a refund unused operation event.
   * 
   * @param account           The account containing billing information.
   * @param quantity          The reported number of operations
   * @param unitAmountDecimal The reported monetary amount
   * @return The persisted log
   */
  public BillingLog refundUnusedOperations(Account account, long quantity, double unitAmountDecimal) {
    var gbp = NumberFormat.getCurrencyInstance(Locale.UK);
    return log(account, "billing.refund", gbp.format(Math.abs((quantity * unitAmountDecimal) / 100)));
  }

  /**
   * Log an invoice payment succeeded event.
   * 
   * @param account       The account containing billing information.
   * @param invoiceNumber The unique invoice number
   * @return The persisted log
   */
  public BillingLog invoicePaymentSucceeded(Account account, String invoiceNumber, long amount) {
    var gbp = NumberFormat.getCurrencyInstance(Locale.UK);
    return log(account, String.format("billing.invoice.%s.payment_succeeded", invoiceNumber),
        gbp.format(-Math.abs(amount / 100)));
  }

  /**
   * Log an invoice payment failed event.
   * 
   * @param account       The account containing billing information.
   * @param invoiceNumber The unique invoice number.
   * @return The persisted log
   */
  public BillingLog invoicePaymentFailed(Account account, String invoiceNumber) {
    return log(account, String.format("billing.invoice.%s.payment_failed", invoiceNumber));
  }

  /**
   * Log a delinquent customer event.
   * 
   * @param account  The account containing billing information.
   * @param nickname The subscription nickname being cancelled.
   * @return The persisted log
   */
  public BillingLog delinquentCustomer(Account account, String nickname) {
    deleteSubscription(account, nickname);
    return log(account, "billing.subscription.suspended");
  }

  private BillingLog log(Account account, String action) {
    return log(account, action, null);
  }

  private BillingLog log(Account account, String action, String amount) {
    return billingLogRepository.save(new BillingLog(null, account, action, amount, OffsetDateTime.now()));
  }
}
