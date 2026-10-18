package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Creates NetBadge accounts in an isolated transaction so uniqueness races are recoverable. */
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserAccountProvisioner {

    private final UserAccountRepository userAccountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserAccount provision(String email, String externalSubject) {
        UserAccount account = UserAccount.fromNetBadgeIdentity(email, externalSubject);
        return userAccountRepository.saveAndFlush(account);
    }
}
