package uk.thepragmaticdev.log.security;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@Service
public class SecurityLogService {

  private HttpServletRequest request;

  private RequestMetadataService requestMetadataService;

  private SecurityLogRepository securityLogRepository;

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
   * @param accountId The id of the account requesting logs
   * @return A list of all logs for the requested account
   */
  public List<SecurityLog> findAllByAccountId(Long accountId) {
    return securityLogRepository.findAllByAccountIdOrderByInstantDesc(accountId);
  }

  /**
   * Find the latest logs for the requested account.
   * 
   * @param pageable  The pagination information
   * @param accountId The id of the account requesting logs
   * @return
   */
  public Page<SecurityLog> findAllByAccountId(Pageable pageable, Long accountId) {
    return securityLogRepository.findAllByAccountIdOrderByInstantDesc(pageable, accountId);
  }

  /**
   * Log a created event for when an account is created.
   * 
   * @param accountId The id of the account being created
   * @return The persisted log
   */
  public SecurityLog created(Long accountId) {
    return log(accountId, "account.created");
  }

  /**
   * Log a password reset event for when an accounts password is reset.
   * 
   * @param accountId The id of the account being updated
   * @return The persisted log
   */
  public SecurityLog reset(Long accountId) {
    return log(accountId, "account.password.reset");
  }

  /**
   * Log an enabled event for when an account email subscription is enabled or
   * disabled.
   * 
   * @param accountId The id of the account email subscription being enabled or
   *                  disabled
   * @param enabled   The enabled status of the accounts email subscription
   * @return The persisted log
   */
  public SecurityLog emailSubscriptionEnabled(Long accountId, boolean enabled) {
    if (enabled) {
      return log(accountId, "account.email_subscription.enabled");
    }
    return log(accountId, "account.email_subscription.disabled");
  }

  /**
   * Log an enabled event for when an account billing alert is enabled or
   * disabled.
   * 
   * @param accountId The id of the account billing alert being enabled or
   *                  disabled
   * @param enabled   The enabled status of the accounts billing alert
   * @return The persisted log
   */
  public SecurityLog billingAlertEnabled(Long accountId, boolean enabled) {
    if (enabled) {
      return log(accountId, "account.billing_alert.enabled");
    }
    return log(accountId, "account.billing_alert.disabled");
  }

  /**
   * Log a fullname event for when an accounts fullname is updated.
   * 
   * @param accountId The id of the account being created
   * @return The persisted log
   */
  public SecurityLog fullname(Long accountId) {
    return log(accountId, "account.fullname.changed");
  }

  private SecurityLog log(Long accountId, String action) {
    var ip = requestMetadataService.extractRequestMetadata(request).map(r -> r.getIp()).orElse("");
    return securityLogRepository.save(new SecurityLog(null, accountId, action, ip, OffsetDateTime.now()));
  }
}
