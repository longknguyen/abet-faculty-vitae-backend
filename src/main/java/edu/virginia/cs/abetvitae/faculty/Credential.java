package edu.virginia.cs.abetvitae.faculty;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="credential")
public class Credential {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(name="credential_type",nullable = false, length = 20)
    private CredentialType credentialType;

    @Column(name="name", nullable = false, length = 255)
    private String name;

    @Column(name="issuing_organisation", nullable = false, length = 100)
    private String issuingOrganisation;

    @Column(name="credential_number", nullable = false, length = 200)
    private String credentialNumber;

    @Column(name="issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name="expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
