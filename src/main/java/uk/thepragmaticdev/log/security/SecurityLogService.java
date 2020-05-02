package uk.thepragmaticdev.log.security;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.security.request.RequestMetadata;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@Service
public class SecurityLogService {

  private final HttpServletRequest request;

  private final RequestMetadataService requestMetadataService;

  private final SecurityLogRepository securityLogRepository;

  /**
   * Service for logging security events such as updates to account.
   * 
   * @param request                The request information for HTTP servlets
   * @param requestMetadataService The service for gathering ip and location
   *                               information
   * @param securityLogRepository  The data access repository for security logs
   */
  @Autowired
  public SecurityLogService(//
      HttpServletRequest request, //
      RequestMetadataService requestMetadataService, //
      SecurityLogRepository securityLogRepository) {
    this.request = request;
    this.requestMetadataService = requestMetadataService;
    this.securityLogRepository = securityLogRepository;
  }

  /**
   * Find all logs for the requested account.
   * 
   * @param account The account requesting logs
   * @return A list of all logs for the requested account
   */
  public List<SecurityLog> findAllByAccountId(Account account) {
    return securityLogRepository.findAllByAccountIdOrderByCreatedDateDesc(account.getId());
  }

  /**
   * Find the latest logs for the requested account.
   * 
   * @param pageable The pagination information
   * @param account  The account requesting logs
   * @return
   */
  public Page<SecurityLog> findAllByAccountId(Pageable pageable, Account account) {
    return securityLogRepository.findAllByAccountIdOrderByCreatedDateDesc(pageable, account.getId());
  }

  /**
   * Log a created event for when an account is created.
   * 
   * @param account The account being created
   * @return The persisted log
   */
  public SecurityLog created(Account account) {
    return log(account, "account.created");
  }

  /**
   * Log a password reset event for when an accounts password is reset.
   * 
   * @param account The account being updated
   * @return The persisted log
   */
  public SecurityLog reset(Account account) {
    return log(account, "account.password.reset");
  }

  /**
   * Log an enabled event for when an account email subscription is enabled or
   * disabled.
   * 
   * @param account The account email subscription being enabled or disabled
   * @param enabled The enabled status of the accounts email subscription
   * @return The persisted log
   */
  public SecurityLog emailSubscriptionEnabled(Account account, boolean enabled) {
    if (enabled) {
      return log(account, "account.email_subscription.enabled");
    }
    return log(account, "account.email_subscription.disabled");
  }

  /**
   * Log an enabled event for when an account billing alert is enabled or
   * disabled.
   * 
   * @param account The account billing alert being enabled or disabled
   * @param enabled The enabled status of the accounts billing alert
   * @return The persisted log
   */
  public SecurityLog billingAlertEnabled(Account account, boolean enabled) {
    if (enabled) {
      return log(account, "account.billing_alert.enabled");
    }
    return log(account, "account.billing_alert.disabled");
  }

  /**
   * Log a fullname event for when an accounts fullname is updated.
   * 
   * @param account The account being created
   * @return The persisted log
   */
  public SecurityLog fullname(Account account) {
    return log(account, "account.fullname.changed");
  }

  /**
   * Log a signin.
   * 
   * @param account The account signing in
   * @return The persisted log
   */
  public SecurityLog signin(Account account) {
    return log(account, "account.signin");
  }

  /**
   * Log a signin from an unrecognized device.
   * 
   * @param account The account signing in
   * @return The persisted log
   */
  public SecurityLog unrecognizedDevice(Account account, RequestMetadata requestMetadata) {
    return log(account, "account.signin.unrecognized_device", requestMetadata);
  }

  /**
   * Log when an accounts billing logs have been downloaded.
   * 
   * @param account The account downloading logs
   * @return The persisted log
   */
  public SecurityLog downloadBillingLogs(Account account) {
    return log(account, "account.download.billing_logs");
  }

  /**
   * Log when an accounts security logs have been downloaded.
   * 
   * @param account The account downloading logs
   * @return The persisted log
   */
  public SecurityLog downloadSecurityLogs(Account account) {
    return log(account, "account.download.security_logs");
  }

  /**
   * Log when an api key has been created from the account.
   * 
   * @param account The account associated with the api key
   * @param key     The api key that was created
   * @return The persisted log
   */
  public SecurityLog createKey(Account account, ApiKey key) {
    return log(account, String.format("account.key.%s.created", key.getPrefix()));
  }

  /**
   * Log when an api key has been deleted from the account. The key logs will have
   * been deleted hence we log this event in the security logs.
   * 
   * @param account The account associated with the api key
   * @param key     The api key that was deleted
   * @return The persisted log
   */
  public SecurityLog deleteKey(Account account, ApiKey key) {
    return log(account, String.format("account.key.%s.deleted", key.getPrefix()));
  }

  private SecurityLog log(Account account, String action) {
    var requestMetadata = requestMetadataService.extractRequestMetadata(request).orElse(null);
    return log(account, action, requestMetadata);
  }

  private SecurityLog log(Account account, String action, RequestMetadata requestMetadata) {
    return securityLogRepository.save(new SecurityLog(null, account, action, requestMetadata, OffsetDateTime.now()));
  }
}
