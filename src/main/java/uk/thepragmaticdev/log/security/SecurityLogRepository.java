package uk.thepragmaticdev.log.security;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

  List<SecurityLog> findAllByAccountIdOrderByCreatedDateDesc(Long accountId);

  Page<SecurityLog> findAllByAccountIdOrderByCreatedDateDesc(Pageable pageable, Long accountId);
}
