package edu.virginia.cs.abetvitae.account.repository;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByExternalSubject(String externalSubject);

    boolean existsByEmailIgnoreCase(String email);
}
