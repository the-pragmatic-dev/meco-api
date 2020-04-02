package uk.thepragmaticdev.log.billing;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.security.RequestService;

@Service
public class BillingLogService {

  private HttpServletRequest request;

  private RequestService requestService;

  private BillingLogRepository billingLogRepository;

  /**
   * TODO.
   * 
   * @param request              TODO
   * @param requestService       TODO
   * @param billingLogRepository TODO
   */
  @Autowired
  public BillingLogService(//
      HttpServletRequest request, //
      RequestService requestService, //
      BillingLogRepository billingLogRepository) {
    this.request = request;
    this.requestService = requestService;
    this.billingLogRepository = billingLogRepository;
  }

  /**
   * TODO.
   * 
   * @param accountId TODO
   * @return
   */
  public List<BillingLog> findAllByAccountId(Long accountId) {
    return billingLogRepository.findAllByAccountIdOrderByInstantDesc(accountId);
  }

  /**
   * TODO.
   * 
   * @param pageable  TODO
   * @param accountId TODO
   * @return
   */
  public Page<BillingLog> findAllByAccountId(Pageable pageable, Long accountId) {
    return billingLogRepository.findAllByAccountIdOrderByInstantDesc(pageable, accountId);
  }

  /**
   * TODO.
   * 
   * @param accountId TODO
   * @return
   */
  public BillingLog invoice(Long accountId) {
    return log(accountId, "billing.invoice");
  }

  private BillingLog log(Long accountId, String action) {
    return billingLogRepository
        .save(new BillingLog(null, accountId, action, requestService.getClientIp(request), OffsetDateTime.now()));
  }
}
