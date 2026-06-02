package com.skillmap.service;

import com.skillmap.dto.InsightDTO;
import com.skillmap.entity.SkillTrend;
import com.skillmap.repository.JobOfferRepository;
import com.skillmap.repository.SkillTrendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyse IA légère : alertes de croissance, corrélations et tendances.
 * Résultat mis en cache Redis 1h (cache "insights").
 */
@Service
@RequiredArgsConstructor
public class InsightService {

    /** Seuil métier : au-delà, on déclenche une alerte de forte hausse. */
    private static final double GROWTH_ALERT_THRESHOLD = 20.0;
    /** Seuil métier : en deçà, la compétence est en recul. */
    private static final double DECLINE_THRESHOLD = -10.0;
    /** Nombre de tendances examinées pour bâtir les insights. */
    private static final int SCAN_SIZE = 50;

    private final SkillTrendRepository trendRepo;
    private final JobOfferRepository jobOfferRepo;

    @Cacheable(value = "insights")
    public List<InsightDTO> getInsights() {
        String currentMonth = YearMonth.now().toString();
        List<SkillTrend> trends = trendRepo.findTopByYearMonthOrderByMentionCountDesc(
            currentMonth, PageRequest.of(0, SCAN_SIZE));

        List<InsightDTO> insights = new ArrayList<>();

        // ── Alertes : compétences en forte hausse ───────────────────
        trends.stream()
            .filter(t -> growth(t) > GROWTH_ALERT_THRESHOLD)
            .limit(3)
            .forEach(t -> insights.add(new InsightDTO(
                t.getSkill().getName() + " en forte hausse",
                String.format("%s a augmenté de %.0f%% ce mois-ci.",
                    t.getSkill().getName(), growth(t)),
                "ALERT", "🚀")));

        // ── Tendance : compétence la plus demandée ───────────────────
        trends.stream().findFirst().ifPresent(top -> insights.add(new InsightDTO(
            "Compétence la plus demandée",
            String.format("%s domine le marché avec %d offres ce mois.",
                top.getSkill().getName(), mentions(top)),
            "TREND", "📈")));

        // ── Corrélation : combo le plus fréquent ─────────────────────
        trends.stream().findFirst().ifPresent(top -> {
            int baseMentions = mentions(top);
            if (baseMentions <= 0) return;
            jobOfferRepo.findCoOccurringSkills(top.getSkill().getName(), PageRequest.of(0, 1))
                .stream().findFirst().ifPresent(row -> {
                    String partner = (String) row[0];
                    long coCount = ((Number) row[1]).longValue();
                    double pct = Math.min(100.0, Math.round(coCount * 100.0 / baseMentions));
                    insights.add(new InsightDTO(
                        "Combo gagnant",
                        String.format("%s est souvent demandé avec %s (%.0f%% des offres).",
                            top.getSkill().getName(), partner, pct),
                        "CORRELATION", "🔗"));
                });
        });

        // ── Tendance : compétence en recul ───────────────────────────
        trends.stream()
            .filter(t -> growth(t) < DECLINE_THRESHOLD)
            .findFirst()
            .ifPresent(t -> insights.add(new InsightDTO(
                t.getSkill().getName() + " en recul",
                String.format("%s recule de %.0f%% — la demande faiblit.",
                    t.getSkill().getName(), Math.abs(growth(t))),
                "TREND", "📉")));

        if (insights.isEmpty()) {
            insights.add(new InsightDTO(
                "Analyse en cours",
                "Les données sont en cours de collecte. Revenez bientôt pour les premiers insights.",
                "TREND", "⏳"));
        }

        return insights;
    }

    private static double growth(SkillTrend t) {
        return t.getGrowthRate() != null ? t.getGrowthRate() : 0.0;
    }

    private static int mentions(SkillTrend t) {
        return t.getMentionCount() != null ? t.getMentionCount() : 0;
    }
}
