package com.skillmap.repository;

import com.skillmap.entity.JobOffer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Accès aux offres d'emploi brutes.
 * Sert à la déduplication (collecteurs) et aux agrégats (services).
 */
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    /** Déduplication : vrai si une offre avec cet externalId existe déjà. */
    boolean existsByExternalId(String externalId);

    /**
     * Répartition géographique d'une compétence.
     * @return lignes [city (String), count (Long)] triées par count décroissant.
     */
    @Query("""
        SELECT j.city, COUNT(j)
        FROM JobOffer j JOIN j.skills s
        WHERE s.name = :skillName AND j.city IS NOT NULL
        GROUP BY j.city
        ORDER BY COUNT(j) DESC
        """)
    List<Object[]> countBySkillNameGroupByCity(@Param("skillName") String skillName);

    /**
     * Répartition géographique toutes compétences confondues.
     * @return lignes [city (String), count (Long)] triées par count décroissant.
     */
    @Query("""
        SELECT j.city, COUNT(j)
        FROM JobOffer j
        WHERE j.city IS NOT NULL
        GROUP BY j.city
        ORDER BY COUNT(j) DESC
        """)
    List<Object[]> countByCity();

    /** Top compétences d'une ville (pour CityDTO.topSkills). */
    @Query("""
        SELECT s.name
        FROM JobOffer j JOIN j.skills s
        WHERE j.city = :city
        GROUP BY s.name
        ORDER BY COUNT(j) DESC
        """)
    List<String> topSkillsByCity(@Param("city") String city, Pageable pageable);

    /**
     * Nombre d'offres par compétence pour un mois donné (format "2026-06").
     * Base du calcul des tendances mensuelles.
     * @return lignes [skillName (String), count (Long)] triées par count décroissant.
     */
    @Query("""
        SELECT s.name, COUNT(j)
        FROM JobOffer j JOIN j.skills s
        WHERE FUNCTION('to_char', j.postedAt, 'YYYY-MM') = :yearMonth
        GROUP BY s.name
        ORDER BY COUNT(j) DESC
        """)
    List<Object[]> countSkillsByMonth(@Param("yearMonth") String yearMonth);

    /**
     * Compétences fréquemment associées à une compétence donnée (co-occurrence).
     * Sert aux insights de corrélation.
     * @return lignes [skillName (String), count (Long)] triées par count décroissant.
     */
    @Query("""
        SELECT s2.name, COUNT(j)
        FROM JobOffer j JOIN j.skills s1 JOIN j.skills s2
        WHERE s1.name = :skillName AND s2.name <> :skillName
        GROUP BY s2.name
        ORDER BY COUNT(j) DESC
        """)
    List<Object[]> findCoOccurringSkills(@Param("skillName") String skillName, Pageable pageable);

    /** Nombre de villes distinctes couvertes (stats globales). */
    @Query("SELECT COUNT(DISTINCT j.city) FROM JobOffer j WHERE j.city IS NOT NULL")
    long countDistinctCities();

    /** Nombre d'offres publiées pour un mois donné (format "2026-06"). */
    @Query("SELECT COUNT(j) FROM JobOffer j WHERE FUNCTION('to_char', j.postedAt, 'YYYY-MM') = :yearMonth")
    long countByMonth(@Param("yearMonth") String yearMonth);

    /** Date de la dernière collecte (stats globales : lastUpdated). */
    @Query("SELECT MAX(j.collectedAt) FROM JobOffer j")
    Optional<LocalDateTime> findLastCollectedAt();
}
