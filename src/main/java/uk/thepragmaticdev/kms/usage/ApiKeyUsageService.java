package uk.thepragmaticdev.kms.usage;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.kms.ApiKey;

@Service
public class ApiKeyUsageService {

  private final ApiKeyUsageRepository apiKeyUsageRepository;

  /**
   * Service for retrieving api key usage records.
   * 
   * @param apiKeyUsageRepository The data access repository for key usages
   */
  @Autowired
  public ApiKeyUsageService(ApiKeyUsageRepository apiKeyUsageRepository) {
    this.apiKeyUsageRepository = apiKeyUsageRepository;
  }

  /**
   * Find all api key usage records between two given dates.
   * 
   * @param apiKey The currently authenticated api key
   * @param from   An inclusive lower usage date
   * @param to     An inclusive upper usage date
   * @return
   */
  public List<ApiKeyUsage> findByApiKeyAndRange(ApiKey apiKey, LocalDate from, LocalDate to) {
    return apiKeyUsageRepository.findByApiKeyIdAndUsageDateBetween(apiKey.getId(), from, to);
  }

  /**
   * Find all api key usage records between two given dates for given ids.
   * 
   * @param ids  A list of relevant api key ids
   * @param from An inclusive lower usage date
   * @param to   An inclusive upper usage date
   * @return
   */
  public List<ApiKeyUsage> findByApiKeyInAndRange(List<Long> ids, LocalDate from, LocalDate to) {
    return apiKeyUsageRepository.findByApiKeyIdInAndUsageDateBetween(ids, from, to);
  }

  /**
   * For a given list of api key usages, sum the total operations.
   * 
   * @param usages A list of usages to sum.
   * @return The total number of operations.
   */
  public long count(List<ApiKeyUsage> usages) {
    return usages.stream().mapToLong(usage -> {
      return usage.getImageOperations() + usage.getTextOperations();
    }).sum();
  }
}
