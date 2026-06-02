package com.skillmap.repository;

import com.skillmap.entity.Skill;
import com.skillmap.entity.SkillTrend;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux agrégats mensuels (tendances) par compétence.
 */
public interface SkillTrendRepository extends JpaRepository<SkillTrend, Long> {

    /**
     * Évolution d'une compétence entre deux mois inclus (format "2026-06").
     * Les yearMonth étant au format ISO triable, le BETWEEN lexicographique est correct.
     */
    List<SkillTrend> findBySkill_NameAndYearMonthBetweenOrderByYearMonthAsc(
        String skillName, String startMonth, String endMonth);

    /** Tendance d'une compétence pour un mois précis (calcul de croissance). */
    Optional<SkillTrend> findBySkill_NameAndYearMonth(String skillName, String yearMonth);

    /** Tendance d'une compétence (entité) pour un mois précis (upsert lors du calcul). */
    Optional<SkillTrend> findBySkillAndYearMonth(Skill skill, String yearMonth);

    /**
     * Top compétences d'un mois, triées par nombre de mentions décroissant.
     * La taille est contrôlée via Pageable.
     */
    @Query("""
        SELECT t FROM SkillTrend t JOIN FETCH t.skill
        WHERE t.yearMonth = :yearMonth
        ORDER BY t.mentionCount DESC
        """)
    List<SkillTrend> findTopByYearMonthOrderByMentionCountDesc(
        @Param("yearMonth") String yearMonth, Pageable pageable);

    /** Top compétences d'un mois filtrées par catégorie. */
    @Query("""
        SELECT t FROM SkillTrend t JOIN FETCH t.skill s
        WHERE t.yearMonth = :yearMonth AND s.category = :category
        ORDER BY t.mentionCount DESC
        """)
    List<SkillTrend> findTopByYearMonthAndCategory(
        @Param("yearMonth") String yearMonth, @Param("category") String category, Pageable pageable);
}
