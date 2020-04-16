package uk.thepragmaticdev.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByUsername(String username);

  Optional<Account> findByPasswordResetToken(String passwordResetToken);

  boolean existsByUsername(String username);
}
