package uk.thepragmaticdev.log.billing;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingLogRepository extends JpaRepository<BillingLog, Long> {

  List<BillingLog> findAllByAccountIdOrderByInstantDesc(Long accountId);

  Page<BillingLog> findAllByAccountIdOrderByInstantDesc(Pageable pageable, Long accountId);
}
