package uk.thepragmaticdev.kms.usage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyUsageRepository extends JpaRepository<ApiKeyUsage, Long> {

  Optional<ApiKeyUsage> findOneByApiKeyIdAndUsageDate(Long apiKeyId, LocalDate usageDate);

  List<ApiKeyUsage> findByApiKeyIdAndUsageDateBetween(Long apiKeyId, LocalDate to, LocalDate from);

  List<ApiKeyUsage> findByApiKeyIdInAndUsageDateBetween(List<Long> ids, LocalDate from, LocalDate to);
}
