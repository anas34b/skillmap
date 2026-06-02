package com.skillmap.dto;

// Insight IA
public record InsightDTO(
    String title,
    String description,
    String type,         // "TREND", "CORRELATION", "ALERT"
    String icon          // emoji
) {}
