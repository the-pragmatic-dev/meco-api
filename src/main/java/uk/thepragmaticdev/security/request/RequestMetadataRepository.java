package uk.thepragmaticdev.security.request;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestMetadataRepository extends JpaRepository<RequestMetadata, Long> {

  List<RequestMetadata> findByAccountId(long accountId);
}
