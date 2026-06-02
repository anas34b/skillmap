package com.skillmap.entity;

import jakarta.persistence.*;
import lombok.*;

// ─────────────────────────────────────────
//  SKILL — Compétence tech détectée
// ─────────────────────────────────────────
@Entity
@Table(name = "skills")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom normalisé ex: "Java", "Docker", "Angular"
    @Column(nullable = false, unique = true)
    private String name;

    // Catégorie : LANGUAGE, FRAMEWORK, DEVOPS, CLOUD, DATABASE, etc.
    private String category;
}
