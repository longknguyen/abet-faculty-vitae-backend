package edu.virginia.cs.abetvitae.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "user_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_account_email",
                columnNames = "email"
        )
)
public class UserAccount {

    public static UserAccount fromNetBadgeIdentity(String email, String externalSubject) {
        UserAccount account = new UserAccount();
        account.email = email.strip().toLowerCase(Locale.ROOT);
        account.externalSubject = externalSubject.strip();
        return account;
    }

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "external_subject", length = 255, unique = true)
    private String externalSubject;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
