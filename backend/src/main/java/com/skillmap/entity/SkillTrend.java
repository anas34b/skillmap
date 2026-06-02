package com.skillmap.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.YearMonth;

// ─────────────────────────────────────────
//  SKILL TREND — Tendance mensuelle
// ─────────────────────────────────────────
@Entity
@Table(name = "skill_trends",
    uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "year_month"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SkillTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    // Format : "2026-06"
    @Column(name = "year_month", nullable = false)
    private String yearMonth;

    // Nombre d'offres mentionnant cette compétence ce mois
    @Column(name = "mention_count")
    private Integer mentionCount;

    // % de croissance vs mois précédent
    @Column(name = "growth_rate")
    private Double growthRate;

    // Top ville ce mois pour cette compétence
    @Column(name = "top_city")
    private String topCity;
}
