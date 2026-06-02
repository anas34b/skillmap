package com.skillmap.dto;

// Statistiques globales dashboard
public record StatsDTO(
    long totalJobsAnalyzed,
    int totalSkillsTracked,
    int totalCitiesCovered,
    String lastUpdated,
    int newJobsThisMonth
) {}
