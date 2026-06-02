package com.skillmap.dto;

// Top compétences — Record Java 21 (immuable, zéro boilerplate)
public record SkillDTO(
    String name,
    String category,
    int mentionCount,
    double growthRate,   // % vs mois précédent
    String trend         // "UP", "DOWN", "STABLE"
) {}
