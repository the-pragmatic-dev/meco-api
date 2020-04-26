package uk.thepragmaticdev.log.key;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@Service
public class ApiKeyLogService {

  private HttpServletRequest request;

  private RequestMetadataService requestMetadataService;

  private ApiKeyLogRepository apiKeyLogRepository;

  /**
   * Service for logging key events such as updates and key usage.
   * 
   * @param request                The request information for HTTP servlets
   * @param requestMetadataService The service for gathering ip and location
   *                               information
   * @param apiKeyLogRepository    The data access repository for key logs
   */
  @Autowired
  public ApiKeyLogService(//
      HttpServletRequest request, //
      RequestMetadataService requestMetadataService, //
      ApiKeyLogRepository apiKeyLogRepository) {
    this.request = request;
    this.requestMetadataService = requestMetadataService;
    this.apiKeyLogRepository = apiKeyLogRepository;
  }

  /**
   * Find all logs for the requested api key.
   * 
   * @param apiKeyId The id of the key requesting logs
   * @return A list of all logs for the requested api key
   */
  public List<ApiKeyLog> findAllByApiKeyId(Long apiKeyId) {
    return apiKeyLogRepository.findAllByApiKeyIdOrderByInstantDesc(apiKeyId);
  }

  /**
   * Find the latest logs for the requested key id.
   * 
   * @param pageable The pagination information
   * @param apiKeyId The id of the key requesting logs
   * @return A page of the latest requested key logs
   */
  public Page<ApiKeyLog> findAllByApiKeyId(Pageable pageable, Long apiKeyId) {
    return apiKeyLogRepository.findAllByApiKeyIdOrderByInstantDesc(pageable, apiKeyId);
  }

  /**
   * Delete all logs for the given api key.
   * 
   * @param apiKeyId The id of the api key for deleting logs.
   */
  public void delete(Long apiKeyId) {
    apiKeyLogRepository.deleteAllByApiKeyId(apiKeyId);
  }

  /**
   * Log a created event for when a key is created.
   * 
   * @param apiKeyId The id of the key being created
   * @return The persisted log
   */
  public ApiKeyLog created(Long apiKeyId) {
    return log(apiKeyId, "key.created");
  }

  /**
   * Log an enabled event for when a key is enabled or disabled.
   * 
   * @param apiKeyId The id of the key being enabled or disabled
   * @param enabled  The enabled status of the key
   * @return The persisted log
   */
  public ApiKeyLog enabled(Long apiKeyId, boolean enabled) {
    if (enabled) {
      return log(apiKeyId, "key.enabled");
    }
    return log(apiKeyId, "key.disabled");
  }

  /**
   * Log a scope event for when any of the keys scope changes.
   * 
   * @param apiKeyId  The id of the keys scope being updated.
   * @param scopeName The name of the scope
   * @param enabled   The enabled status of the keys scope
   * @return The persisted log
   */
  public ApiKeyLog scope(Long apiKeyId, String scopeName, boolean enabled) {
    if (enabled) {
      return log(apiKeyId, String.format("key.scope.%s.enabled", scopeName));
    }
    return log(apiKeyId, String.format("key.scope.%s.disabled", scopeName));
  }

  private ApiKeyLog log(Long apiKeyId, String action) {
    var ip = requestMetadataService.extractRequestMetadata(request).map(r -> r.getIp()).orElse("");
    return apiKeyLogRepository.save(new ApiKeyLog(null, apiKeyId, action, ip, OffsetDateTime.now()));
  }
}
