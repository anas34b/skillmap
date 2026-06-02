package com.skillmap.service;

import com.skillmap.dto.StatsDTO;
import com.skillmap.repository.JobOfferRepository;
import com.skillmap.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Statistiques globales affichées dans le header du dashboard.
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter UPDATED_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JobOfferRepository jobOfferRepo;
    private final SkillRepository skillRepo;

    @Cacheable(value = "globalStats")
    public StatsDTO getGlobalStats() {
        long totalJobs = jobOfferRepo.count();
        int totalSkills = (int) skillRepo.count();
        int totalCities = (int) jobOfferRepo.countDistinctCities();
        int newJobsThisMonth = (int) jobOfferRepo.countByMonth(YearMonth.now().toString());

        String lastUpdated = jobOfferRepo.findLastCollectedAt()
            .map(UPDATED_FMT::format)
            .orElse("N/A");

        return new StatsDTO(totalJobs, totalSkills, totalCities, lastUpdated, newJobsThisMonth);
    }
}
