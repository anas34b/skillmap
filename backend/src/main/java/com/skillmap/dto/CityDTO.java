package com.skillmap.dto;

import java.util.List;

// Répartition géographique
public record CityDTO(
    String city,
    int jobCount,
    double percentage,
    List<String> topSkills
) {}
