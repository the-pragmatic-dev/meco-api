package uk.thepragmaticdev.log.key;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyLogRepository extends JpaRepository<ApiKeyLog, Long> {

  List<ApiKeyLog> findAllByApiKeyIdOrderByInstantDesc(Long apiKeyId);

  Page<ApiKeyLog> findAllByApiKeyIdOrderByInstantDesc(Pageable pageable, Long apiKeyId);
}
