package uk.thepragmaticdev.log.security;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.security.RequestService;

@Service
public class SecurityLogService {

  private HttpServletRequest request;

  private RequestService requestService;

  private SecurityLogRepository securityLogRepository;

  /**
   * TODO.
   * 
   * @param request               TODO
   * @param requestService        TODO
   * @param securityLogRepository TODO
   */
  @Autowired
  public SecurityLogService(//
      HttpServletRequest request, //
      RequestService requestService, //
      SecurityLogRepository securityLogRepository) {
    this.request = request;
    this.requestService = requestService;
    this.securityLogRepository = securityLogRepository;
  }

  public List<SecurityLog> findAllByAccountId(Long accountId) {
    return securityLogRepository.findAllByAccountIdOrderByInstantDesc(accountId);
  }

  public Page<SecurityLog> findAllByAccountId(Pageable pageable, Long accountId) {
    return securityLogRepository.findAllByAccountIdOrderByInstantDesc(pageable, accountId);
  }

  public SecurityLog created(Long accountId) {
    return log(accountId, "account.created");
  }

  /**
   * TODO.
   * 
   * @param accountId TODO
   * @param enabled   TODO
   * @return
   */
  public SecurityLog emailSubscriptionEnabled(Long accountId, boolean enabled) {
    if (enabled) {
      return log(accountId, "account.email_subscription.enabled");
    }
    return log(accountId, "account.email_subscription.disabled");
  }

  /**
   * TODO.
   * 
   * @param accountId TODO
   * @param enabled   TODO
   * @return
   */
  public SecurityLog billingAlertEnabled(Long accountId, boolean enabled) {
    if (enabled) {
      return log(accountId, "account.billing_alert.enabled");
    }
    return log(accountId, "account.billing_alert.disabled");
  }

  /**
   * TODO.
   * 
   * @param accountId TODO
   * @return
   */
  public SecurityLog fullname(Long accountId) {
    return log(accountId, "account.fullname.changed");
  }

  private SecurityLog log(Long accountId, String action) {
    return securityLogRepository
        .save(new SecurityLog(null, accountId, action, requestService.getClientIp(request), OffsetDateTime.now()));
  }
}
