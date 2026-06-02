package com.skillmap.dto;

// Évolution mensuelle d'une compétence
public record TrendDTO(
    String yearMonth,    // "2026-06"
    String skillName,
    int mentionCount,
    double growthRate
) {}
