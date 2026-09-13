package edu.virginia.cs.abetvitae.faculty.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "credential")
public class Credential {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 20)
    private CredentialType credentialType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "issuing_organisation", nullable = true, length = 100)
    private String issuingOrganisation;

    @Column(name = "credential_number", nullable = true, length = 200)
    private String credentialNumber;

    @Column(name = "issue_date", nullable = true)
    private LocalDate issueDate;

    @Column(name = "expiration_date", nullable = true)
    private LocalDate expirationDate;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
