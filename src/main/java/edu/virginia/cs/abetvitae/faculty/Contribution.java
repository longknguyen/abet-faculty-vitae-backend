package edu.virginia.cs.abetvitae.faculty;

import jakarta.persistence.*;
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
@Table(name = "contribution")
public class Contribution {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(name="contribution_type",nullable = false, length = 20)
    private ContributionType contributionType;

    @Column(name="title", nullable = false, length = 255)
    private String title;

    @Column(name="citation", nullable = true, length = 500)
    private String citation;

    @Column(name="venue_or_organisation", nullable = true, length = 100)
    private String venueOrOrganisation;

    @Column(name="contribution_date", nullable = true)
    private LocalDate contributionDate;

    @Column(name="description", nullable = true, length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

}
