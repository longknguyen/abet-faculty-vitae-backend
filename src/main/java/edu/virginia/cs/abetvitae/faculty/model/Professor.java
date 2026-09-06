package edu.virginia.cs.abetvitae.faculty.model;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "professor")
public class Professor {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_account_id", nullable = true, unique = true)
    private UserAccount userAccount;

    @Column(nullable = false, length = 100)
    private String givenName;

    @Column(nullable = true, length = 100)
    private String middleName;

    @Column(nullable = false, length = 100)
    private String familyName;

    @Column(nullable = true, length = 50)
    private String suffix;

    @Column(nullable = true, length = 255)
    private String displayNameOverride;

    @Column(nullable = false, length = 320)
    private String email;

    public String getDisplayName() {
        if (displayNameOverride != null && !displayNameOverride.isBlank()) {
            return displayNameOverride;
        }

        return Stream.of(givenName, middleName, familyName, suffix)
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}
