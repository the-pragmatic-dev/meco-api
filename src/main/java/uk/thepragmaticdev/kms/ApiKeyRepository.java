package uk.thepragmaticdev.kms;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  List<ApiKey> findAllByAccountId(Long accountId);

  Optional<ApiKey> findOneByIdAndAccountId(Long id, Long accountId);

  List<ApiKey> findByPrefix(String prefix);

  long countByAccountId(Long accountId);
}
