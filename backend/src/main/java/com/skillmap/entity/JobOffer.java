package com.skillmap.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ─────────────────────────────────────────
//  JOBOF FER — Offre d'emploi brute
// ─────────────────────────────────────────
@Entity
@Table(name = "job_offers",
    indexes = {
        @Index(name = "idx_job_city", columnList = "city"),
        @Index(name = "idx_job_source", columnList = "source"),
        @Index(name = "idx_job_posted_at", columnList = "posted_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String city;
    private String department;   // ex: "75 - Paris"

    @Column(name = "contract_type")
    private String contractType; // CDI, CDD, ALTERNANCE, STAGE, FREELANCE

    @Column(name = "experience_level")
    private String experienceLevel; // JUNIOR, SENIOR, etc.

    @Column(name = "salary_min")
    private Double salaryMin;

    @Column(name = "salary_max")
    private Double salaryMax;

    private Boolean remote;

    // Source : FRANCE_TRAVAIL ou ARBEITNOW
    @Column(nullable = false)
    private String source;

    // ID externe pour éviter les doublons
    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "apply_url")
    private String applyUrl;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "posted_at")
    private LocalDate postedAt;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    // Relation Many-to-Many avec Skill
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "job_offer_skills",
        joinColumns = @JoinColumn(name = "job_offer_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private List<Skill> skills = new ArrayList<>();
}
