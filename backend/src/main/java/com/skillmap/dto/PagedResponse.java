package com.skillmap.dto;

import java.util.List;

// Réponse paginée générique
public record PagedResponse<T>(
    List<T> data,
    int page,
    int totalPages,
    long totalElements
) {}
