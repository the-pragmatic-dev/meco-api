package uk.thepragmaticdev.kms.usage;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.thepragmaticdev.kms.ApiKey;

@Log4j2
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ApiKeyUsageCache {

  private final Clock clock;

  private final ApiKeyUsageRepository apiKeyUsageRepository;

  private ConcurrentHashMap<Long, Operations> usage;

  /**
   * Singleton containing a concurrent hash map to store moderation request
   * counts. These are flushed to the database periodically.
   * 
   * @param clock                 Provides access to current date and time
   * @param apiKeyUsageRepository The data access repository for key usages
   */
  public ApiKeyUsageCache(Clock clock, ApiKeyUsageRepository apiKeyUsageRepository) {
    this.clock = clock;
    this.apiKeyUsageRepository = apiKeyUsageRepository;
  }

  /**
   * Instantiate usage map. Due to autowiring a constructor can be called multiple
   * times. PostConstruct will run only once after cache is initialised.
   */
  @PostConstruct
  public void init() {
    usage = new ConcurrentHashMap<>();
  }

  /**
   * Report operations on an authenticated api key from a moderation request.
   * 
   * @param apiKey          The currently authenticated api key
   * @param textOperations  The total number of text operations
   * @param imageOperations The total number of image operations
   */
  public void put(ApiKey apiKey, long textOperations, long imageOperations) {
    if (usage.containsKey(apiKey.getId())) {
      var existing = usage.get(apiKey.getId());
      existing.setTextOperations(existing.getTextOperations() + textOperations);
      existing.setImageOperations(existing.getImageOperations() + imageOperations);
    } else {
      usage.put(apiKey.getId(), new Operations(apiKey, textOperations, imageOperations));
    }
  }

  /**
   * Scheduled batch insert of cumulative operations for each api key. The usage
   * date is set to run date.
   */
  @Scheduled(fixedRate = 30000)
  public void flush() {
    if (usage.isEmpty()) {
      return;
    }
    var usageCopy = new ConcurrentHashMap<>(usage);
    usage.clear();

    List<ApiKeyUsage> apiKeyUsages = new ArrayList<>();
    var now = LocalDate.now(clock);
    usageCopy.forEach((key, value) -> {
      var persistentApiKeyUsage = apiKeyUsageRepository.findOneByApiKeyIdAndUsageDate(value.getApiKey().getId(), now);

      if (persistentApiKeyUsage.isPresent()) {
        var existing = persistentApiKeyUsage.get();
        existing.setTextOperations(existing.getTextOperations() + value.getTextOperations());
        existing.setImageOperations(existing.getImageOperations() + value.getImageOperations());
        apiKeyUsages.add(existing);
      } else {
        apiKeyUsages.add(new ApiKeyUsage(//
            null, //
            now, //
            value.getTextOperations(), //
            value.getImageOperations(), //
            value.getApiKey()//
        ));
      }
    });
    apiKeyUsageRepository.saveAll(apiKeyUsages);
    log.info("Flushed {} API key usage record(s)", apiKeyUsages.size());
  }

  @Data
  @AllArgsConstructor
  private class Operations {
    private ApiKey apiKey;
    private long textOperations;
    private long imageOperations;
  }

}
