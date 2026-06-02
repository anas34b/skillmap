package com.skillmap.repository;

import com.skillmap.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux compétences normalisées.
 */
public interface SkillRepository extends JpaRepository<Skill, Long> {

    /** Recherche par nom normalisé (ex: "Java"). Utilisé par les collecteurs. */
    Optional<Skill> findByName(String name);

    /** Toutes les compétences d'une catégorie (LANGUAGE, FRAMEWORK_BACKEND, ...). */
    List<Skill> findByCategory(String category);
}
