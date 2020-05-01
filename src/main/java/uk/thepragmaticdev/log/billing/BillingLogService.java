package uk.thepragmaticdev.log.billing;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@Service
public class BillingLogService {

  private final HttpServletRequest request;

  private final RequestMetadataService requestMetadataService;

  private final BillingLogRepository billingLogRepository;

  /**
   * Service for logging billing events such as invoice creation.
   * 
   * @param request                The request information for HTTP servlets
   * @param requestMetadataService The service for gathering ip and location
   *                               information
   * @param billingLogRepository   The data access repository for billing logs
   */
  @Autowired
  public BillingLogService(//
      HttpServletRequest request, //
      RequestMetadataService requestMetadataService, //
      BillingLogRepository billingLogRepository) {
    this.request = request;
    this.requestMetadataService = requestMetadataService;
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
   * Log an invoice event for when an invoice is created.
   * 
   * @param account The authenticated account
   * @return The persisted log
   */
  public BillingLog invoice(Account account) {
    return log(account, "billing.invoice");
  }

  private BillingLog log(Account account, String action) {
    var ip = requestMetadataService.extractRequestMetadata(request).map(r -> r.getIp()).orElse("");
    return billingLogRepository.save(new BillingLog(null, account, action, ip, OffsetDateTime.now()));
  }
}
