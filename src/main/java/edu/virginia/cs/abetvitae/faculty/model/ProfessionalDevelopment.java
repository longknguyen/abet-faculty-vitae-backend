package edu.virginia.cs.abetvitae.faculty.model;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "professional_development")
public class ProfessionalDevelopment {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="professor_id", nullable=false)
    private Professor professor;

    @Column(name = "activity_name", nullable = false, length = 20)
    private String activityName;

    @Column(name = "provider", nullable = true, length = 255)
    private String provider;

    @Column(name = "start_date", nullable = true)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = true)
    private LocalDate endDate;

    @Column(name = "description", nullable = true, columnDefinition = "text")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
