package uk.thepragmaticdev.log.key;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.security.RequestService;

@Service
public class ApiKeyLogService {

  private HttpServletRequest request;

  private RequestService requestService;

  private ApiKeyLogRepository apiKeyLogRepository;

  /**
   * TODO.
   * 
   * @param request             TODO
   * @param requestService      TODO
   * @param apiKeyLogRepository TODO
   */
  @Autowired
  public ApiKeyLogService(//
      HttpServletRequest request, //
      RequestService requestService, //
      ApiKeyLogRepository apiKeyLogRepository) {
    this.request = request;
    this.requestService = requestService;
    this.apiKeyLogRepository = apiKeyLogRepository;
  }

  /**
   * TODO.
   * 
   * @param apiKeyId TODO
   * @return
   */
  public List<ApiKeyLog> findAllByApiKeyId(Long apiKeyId) {
    return apiKeyLogRepository.findAllByApiKeyIdOrderByInstantDesc(apiKeyId);
  }

  /**
   * TODO.
   * 
   * @param pageable TODO
   * @param apiKeyId TODO
   * @return
   */
  public Page<ApiKeyLog> findAllByApiKeyId(Pageable pageable, Long apiKeyId) {
    return apiKeyLogRepository.findAllByApiKeyIdOrderByInstantDesc(pageable, apiKeyId);
  }

  /**
   * TODO.
   * 
   * @param apiKeyId TODO
   * @return
   */
  public ApiKeyLog created(Long apiKeyId) {
    return log(apiKeyId, "key.created");
  }

  /**
   * TODO.
   * 
   * @param apiKeyId TODO
   * @param enabled  TODO
   * @return
   */
  public ApiKeyLog enabled(Long apiKeyId, boolean enabled) {
    if (enabled) {
      return log(apiKeyId, "key.enabled");
    }
    return log(apiKeyId, "key.disabled");
  }

  /**
   * TODO.
   * 
   * @param apiKeyId TODO
   * @return
   */
  public ApiKeyLog deleted(Long apiKeyId) {
    return log(apiKeyId, "key.deleted");
  }

  private ApiKeyLog log(Long apiKeyId, String action) {
    return apiKeyLogRepository
        .save(new ApiKeyLog(null, apiKeyId, action, requestService.getClientIp(request), OffsetDateTime.now()));
  }
}
