package uk.thepragmaticdev.log.billing;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@Service
public class BillingLogService {

  private HttpServletRequest request;

  private RequestMetadataService requestMetadataService;

  private BillingLogRepository billingLogRepository;

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
   * @param accountId The id of the account requesting logs
   * @return A list of all logs for the requested account
   */
  public List<BillingLog> findAllByAccountId(Long accountId) {
    return billingLogRepository.findAllByAccountIdOrderByInstantDesc(accountId);
  }

  /**
   * Find the latest logs for the requested account.
   * 
   * @param pageable  The pagination information
   * @param accountId The id of the account requesting logs
   * @return
   */
  public Page<BillingLog> findAllByAccountId(Pageable pageable, Long accountId) {
    return billingLogRepository.findAllByAccountIdOrderByInstantDesc(pageable, accountId);
  }

  /**
   * Log an invoice event for when an invoice is created.
   * 
   * @param accountId The id of the account creating an invoice
   * @return The persisted log
   */
  public BillingLog invoice(Long accountId) {
    return log(accountId, "billing.invoice");
  }

  private BillingLog log(Long accountId, String action) {
    var ip = requestMetadataService.extractRequestMetadata(request).map(r -> r.getIp()).orElse("");
    return billingLogRepository.save(new BillingLog(null, accountId, action, ip, OffsetDateTime.now()));
  }
}
