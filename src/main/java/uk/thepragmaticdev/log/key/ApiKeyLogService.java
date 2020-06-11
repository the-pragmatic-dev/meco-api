package uk.thepragmaticdev.log.key;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@Service
public class ApiKeyLogService {

  private final HttpServletRequest request;

  private final RequestMetadataService requestMetadataService;

  private final ApiKeyLogRepository apiKeyLogRepository;

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
   * @param apiKey The key requesting logs
   * @return A list of all logs for the requested api key
   */
  public List<ApiKeyLog> findAllByApiKeyId(ApiKey apiKey) {
    return apiKeyLogRepository.findAllByApiKeyIdOrderByCreatedDateDesc(apiKey.getId());
  }

  /**
   * Find the latest logs for the requested key id.
   * 
   * @param pageable The pagination information
   * @param apiKey   The key requesting logs
   * @return A page of the latest requested key logs
   */
  public Page<ApiKeyLog> findAllByApiKeyId(Pageable pageable, ApiKey apiKey) {
    return apiKeyLogRepository.findAllByApiKeyIdOrderByCreatedDateDesc(pageable, apiKey.getId());
  }

  /**
   * Log a created event for when a key is created.
   * 
   * @param apiKey The key being created
   * @return The persisted log
   */
  public ApiKeyLog created(ApiKey apiKey) {
    return log(apiKey, "key.created");
  }

  /**
   * Log an updated name event for when a keys name is changed.
   * 
   * @param apiKey The key being updated
   * @return The persisted log
   */
  public ApiKeyLog name(ApiKey apiKey) {
    return log(apiKey, "key.name.updated");
  }

  /**
   * Log an enabled event for when a key is enabled or disabled.
   * 
   * @param apiKey  The key being enabled or disabled
   * @param enabled The enabled status of the key
   * @return The persisted log
   */
  public ApiKeyLog enabled(ApiKey apiKey, boolean enabled) {
    if (enabled) {
      return log(apiKey, "key.enabled");
    }
    return log(apiKey, "key.disabled");
  }

  /**
   * Log an access policy event for when a keys polices are updated.
   * 
   * @param apiKey The key being updated
   * @param action The action being performed on the policy
   * @param range  The policy range
   * @return The persisted log
   */
  public ApiKeyLog accessPolicy(ApiKey apiKey, String action, String range) {
    return log(apiKey, String.format("key.access_policy.%s.%s", range, action));
  }

  /**
   * Log a scope event for when any of the keys scope changes.
   * 
   * @param apiKey    The key being updated.
   * @param scopeName The name of the scope
   * @param enabled   The enabled status of the keys scope
   * @return The persisted log
   */
  public ApiKeyLog scope(ApiKey apiKey, String scopeName, boolean enabled) {
    if (enabled) {
      return log(apiKey, String.format("key.scope.%s.enabled", scopeName));
    }
    return log(apiKey, String.format("key.scope.%s.disabled", scopeName));
  }

  /**
   * Log when logs associated with an api key are downloaded.
   * 
   * @param apiKey The key downloading logs
   * @return The persisted log
   */
  public ApiKeyLog downloadLogs(ApiKey apiKey) {
    return log(apiKey, "key.download.logs");
  }

  private ApiKeyLog log(ApiKey apiKey, String action) {
    var requestMetadata = requestMetadataService.extractRequestMetadata(request).orElse(null);
    return apiKeyLogRepository.save(new ApiKeyLog(null, apiKey, action, requestMetadata, OffsetDateTime.now()));
  }
}
