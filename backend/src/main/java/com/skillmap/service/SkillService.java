package com.skillmap.service;

import com.skillmap.dto.CityDTO;
import com.skillmap.dto.SkillDTO;
import com.skillmap.entity.SkillTrend;
import com.skillmap.repository.JobOfferRepository;
import com.skillmap.repository.SkillTrendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Top compétences du mois et répartition géographique.
 */
@Service
@RequiredArgsConstructor
public class SkillService {

    /** Top villes affichées dans le détail de chaque ville (CityDTO.topSkills). */
    private static final int CITY_TOP_SKILLS = 5;

    private final SkillTrendRepository trendRepo;
    private final JobOfferRepository jobOfferRepo;

    /**
     * Top compétences d'un mois, éventuellement filtrées par catégorie.
     * Les tendances sont précalculées (TrendService.calculateAndSaveTrends).
     */
    @Cacheable(value = "topSkills",
        key = "#limit + '-' + (#month == null ? 'now' : #month) + '-' + (#category == null ? 'all' : #category)")
    public List<SkillDTO> getTopSkills(int limit, String month, String category) {
        String yearMonth = resolveMonth(month);
        var pageable = PageRequest.of(0, limit);

        List<SkillTrend> trends = (category == null || category.isBlank())
            ? trendRepo.findTopByYearMonthOrderByMentionCountDesc(yearMonth, pageable)
            : trendRepo.findTopByYearMonthAndCategory(yearMonth, category, pageable);

        // Collectors.toList() (ArrayList, non-final) et NON Stream.toList() : ce dernier
        // renvoie une List immuable FINALE que le GenericJackson2JsonRedisSerializer
        // (default typing NON_FINAL) ne tague pas → échec de relecture du cache Redis.
        return trends.stream().map(this::toSkillDto).collect(Collectors.toList());
    }

    /**
     * Répartition géographique : si skill est fourni, restreint à cette compétence,
     * sinon toutes offres confondues.
     */
    @Cacheable(value = "cityStats",
        key = "(#skill == null ? 'all' : #skill) + '-' + #limit")
    public List<CityDTO> getCityDistribution(String skill, int limit) {
        List<Object[]> rows = (skill == null || skill.isBlank())
            ? jobOfferRepo.countByCity()
            : jobOfferRepo.countBySkillNameGroupByCity(skill);

        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        var topSkillsPage = PageRequest.of(0, CITY_TOP_SKILLS);

        return rows.stream()
            .limit(limit)
            .map(r -> {
                String city = (String) r[0];
                long count = ((Number) r[1]).longValue();
                double percentage = total > 0 ? round1(count * 100.0 / total) : 0.0;
                List<String> topSkills = jobOfferRepo.topSkillsByCity(city, topSkillsPage);
                return new CityDTO(city, (int) count, percentage, topSkills);
            })
            // ArrayList (non-final) requis pour la sérialisation du cache Redis — cf. getTopSkills.
            .collect(Collectors.toList());
    }

    private SkillDTO toSkillDto(SkillTrend t) {
        double growthRate = t.getGrowthRate() != null ? t.getGrowthRate() : 0.0;
        return new SkillDTO(
            t.getSkill().getName(),
            t.getSkill().getCategory(),
            t.getMentionCount() != null ? t.getMentionCount() : 0,
            growthRate,
            trendLabel(growthRate)
        );
    }

    private static String resolveMonth(String month) {
        return (month == null || month.isBlank()) ? YearMonth.now().toString() : month;
    }

    /** Règle métier : > +10% → UP, < -10% → DOWN, sinon STABLE. */
    static String trendLabel(double growthRate) {
        if (growthRate > 10) return "UP";
        if (growthRate < -10) return "DOWN";
        return "STABLE";
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
