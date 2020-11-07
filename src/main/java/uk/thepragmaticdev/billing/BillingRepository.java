package uk.thepragmaticdev.billing;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingRepository extends JpaRepository<Billing, Long> {

  Optional<Billing> findByCustomerId(String customerId);
}
