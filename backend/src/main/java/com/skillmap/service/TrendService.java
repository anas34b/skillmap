package com.skillmap.service;

import com.skillmap.dto.TrendDTO;
import com.skillmap.entity.Skill;
import com.skillmap.entity.SkillTrend;
import com.skillmap.repository.JobOfferRepository;
import com.skillmap.repository.SkillRepository;
import com.skillmap.repository.SkillTrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * Évolution mensuelle des compétences et calcul des agrégats de tendance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendService {

    private final SkillTrendRepository trendRepo;
    private final SkillRepository skillRepo;
    private final JobOfferRepository jobOfferRepo;

    /**
     * Évolution d'une compétence sur les {@code months} derniers mois (mois courant inclus).
     */
    public List<TrendDTO> getMonthlyTrend(String skill, int months) {
        YearMonth now = YearMonth.now();
        String start = now.minusMonths(Math.max(0, months - 1L)).toString();
        String end = now.toString();

        return trendRepo
            .findBySkill_NameAndYearMonthBetweenOrderByYearMonthAsc(skill, start, end)
            .stream()
            .map(this::toTrendDto)
            .toList();
    }

    /**
     * Comparaison de plusieurs compétences sur la même période.
     */
    public List<TrendDTO> compareSkills(List<String> skills, int months) {
        return skills.stream()
            .flatMap(skill -> getMonthlyTrend(skill, months).stream())
            .toList();
    }

    /**
     * Recalcule et persiste les tendances du mois courant.
     * Appelé après chaque collecte.
     * <p>
     * mentionCount = nombre d'offres du mois contenant la compétence.<br>
     * growthRate   = (mentionCount - moisPrécédent) / moisPrécédent × 100.
     */
    @Transactional
    public void calculateAndSaveTrends() {
        String currentMonth = YearMonth.now().toString();
        String previousMonth = YearMonth.now().minusMonths(1).toString();

        List<Object[]> counts = jobOfferRepo.countSkillsByMonth(currentMonth);
        int updated = 0;

        for (Object[] row : counts) {
            String skillName = (String) row[0];
            int mentionCount = ((Number) row[1]).intValue();

            Skill skill = skillRepo.findByName(skillName).orElse(null);
            if (skill == null) continue; // compétence inconnue du dictionnaire

            int previousCount = trendRepo
                .findBySkill_NameAndYearMonth(skillName, previousMonth)
                .map(t -> t.getMentionCount() != null ? t.getMentionCount() : 0)
                .orElse(0);

            double growthRate = previousCount > 0
                ? round1((mentionCount - previousCount) * 100.0 / previousCount)
                : 0.0;

            String topCity = jobOfferRepo.countBySkillNameGroupByCity(skillName).stream()
                .findFirst()
                .map(r -> (String) r[0])
                .orElse(null);

            SkillTrend trend = trendRepo.findBySkillAndYearMonth(skill, currentMonth)
                .orElseGet(() -> SkillTrend.builder()
                    .skill(skill)
                    .yearMonth(currentMonth)
                    .build());
            trend.setMentionCount(mentionCount);
            trend.setGrowthRate(growthRate);
            trend.setTopCity(topCity);

            trendRepo.save(trend);
            updated++;
        }

        log.info("✅ Tendances calculées pour {} — {} compétences", currentMonth, updated);
    }

    private TrendDTO toTrendDto(SkillTrend t) {
        return new TrendDTO(
            t.getYearMonth(),
            t.getSkill().getName(),
            t.getMentionCount() != null ? t.getMentionCount() : 0,
            t.getGrowthRate() != null ? t.getGrowthRate() : 0.0
        );
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
