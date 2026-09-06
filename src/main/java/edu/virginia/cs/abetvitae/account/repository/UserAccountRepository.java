package edu.virginia.cs.abetvitae.account.repository;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findById(UUID id);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
