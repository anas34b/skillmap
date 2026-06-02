package com.skillmap.controller;

import com.skillmap.dto.*;
import com.skillmap.service.SkillService;
import com.skillmap.service.TrendService;
import com.skillmap.service.InsightService;
import com.skillmap.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SkillMapController {

    private final SkillService skillService;
    private final TrendService trendService;
    private final InsightService insightService;
    private final StatsService statsService;

    /**
     * GET /api/skills?limit=20&month=2026-06&category=LANGUAGE
     * Top compétences du mois
     */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillDTO>> getTopSkills(
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String month,
        @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(skillService.getTopSkills(limit, month, category));
    }

    /**
     * GET /api/trends?skill=Java&months=6
     * Évolution mensuelle d'une compétence
     */
    @GetMapping("/trends")
    public ResponseEntity<List<TrendDTO>> getTrend(
        @RequestParam String skill,
        @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(trendService.getMonthlyTrend(skill, months));
    }

    /**
     * GET /api/cities?skill=Angular&limit=10
     * Répartition géographique
     */
    @GetMapping("/cities")
    public ResponseEntity<List<CityDTO>> getCities(
        @RequestParam(required = false) String skill,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(skillService.getCityDistribution(skill, limit));
    }

    /**
     * GET /api/insights
     * Analyse IA — mis en cache Redis 1h
     */
    @GetMapping("/insights")
    public ResponseEntity<List<InsightDTO>> getInsights() {
        return ResponseEntity.ok(insightService.getInsights());
    }

    /**
     * GET /api/stats
     * Statistiques globales pour le header du dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<StatsDTO> getStats() {
        return ResponseEntity.ok(statsService.getGlobalStats());
    }

    /**
     * GET /api/skills/compare?skills=Java,Python,Go
     * Comparaison de plusieurs compétences
     */
    @GetMapping("/skills/compare")
    public ResponseEntity<List<TrendDTO>> compareSkills(
        @RequestParam List<String> skills,
        @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(trendService.compareSkills(skills, months));
    }
}
