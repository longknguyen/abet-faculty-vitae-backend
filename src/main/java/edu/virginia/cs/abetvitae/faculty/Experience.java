package edu.virginia.cs.abetvitae.faculty;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name="experience")
public class Experience {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name= "professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(name ="experience_type", nullable = false,length=20)
    private ExperienceType experienceType;

    @Column(name="organisation", nullable = false,length=255)
    private String organisation;

    @Column(name="academic_rank", nullable = true,length=100)
    private String academicRank;

    @Column(name="position_title", nullable = true,length=200)
    private String positionTitle;

    @Column(name="start_year", nullable = false)
    private int startYear;

    @Column(name="end_year", nullable = true)
    private int endYear;

    @Column(name="is_current")
    private boolean isCurrent=false;

    @Enumerated(EnumType.STRING)
    @Column(name="employment_type",nullable = false)
    private EmploymentType employmentType;

    @Column(name="display_order", nullable = false)
    private int displayOrder;

}
