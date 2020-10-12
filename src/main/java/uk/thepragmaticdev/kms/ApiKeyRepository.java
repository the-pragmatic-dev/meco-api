package uk.thepragmaticdev.kms;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  List<ApiKey> findAllByAccountIdAndDeletedDateIsNull(Long accountId);

  Optional<ApiKey> findOneByIdAndAccountIdAndDeletedDateIsNull(Long id, Long accountId);

  List<ApiKey> findByPrefixAndDeletedDateIsNull(String prefix);

  long countByAccountIdAndDeletedDateIsNull(Long accountId);
}
