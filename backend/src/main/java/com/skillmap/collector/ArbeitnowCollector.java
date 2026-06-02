package com.skillmap.collector;

import com.skillmap.entity.JobOffer;
import com.skillmap.entity.Skill;
import com.skillmap.parser.SkillParser;
import com.skillmap.repository.JobOfferRepository;
import com.skillmap.repository.SkillRepository;
import com.skillmap.service.TrendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ArbeitnowCollector {

    private static final String API_URL = "https://www.arbeitnow.com/api/job-board-api";
    private static final int MAX_PAGES = 3; // 3 pages × 100 offres = 300 offres/run

    private final RestClient restClient;
    private final SkillParser skillParser;
    private final JobOfferRepository jobOfferRepo;
    private final SkillRepository skillRepo;
    private final TrendService trendService;

    public ArbeitnowCollector(RestClient.Builder builder,
                               SkillParser skillParser,
                               JobOfferRepository jobOfferRepo,
                               SkillRepository skillRepo,
                               TrendService trendService) {
        this.restClient = builder.build();
        this.skillParser = skillParser;
        this.jobOfferRepo = jobOfferRepo;
        this.skillRepo = skillRepo;
        this.trendService = trendService;
    }

    // Décalé de 30 min par rapport à France Travail
    @Scheduled(cron = "0 30 * * * *")
    public void collect() {
        log.info("▶ Démarrage collecte Arbeitnow — {}", LocalDateTime.now());
        int total = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            try {
                var response = restClient.get()
                    .uri(API_URL + "?page=" + page)
                    .retrieve()
                    .body(ArbeitnowResponse.class);

                if (response == null || response.data() == null || response.data().isEmpty()) break;

                for (var job : response.data()) {
                    try {
                        String externalId = "ARBEITNOW_" + job.slug();
                        if (jobOfferRepo.existsByExternalId(externalId)) continue;

                        String description = (job.title() != null ? job.title() : "") + " "
                            + (job.description() != null ? job.description() : "");

                        List<String> skillNames = skillParser.extractSkills(description);
                        List<Skill> skills = skillNames.stream()
                            .map(name -> skillRepo.findByName(name)
                                .orElseGet(() -> skillRepo.save(
                                    Skill.builder()
                                        .name(name)
                                        .category(skillParser.getCategory(name))
                                        .build()
                                )))
                            .toList();

                        JobOffer jobOffer = JobOffer.builder()
                            .externalId(externalId)
                            .title(job.title())
                            .description(job.description())
                            .city(job.location())
                            .contractType(job.job_types() != null && !job.job_types().isEmpty()
                                ? job.job_types().get(0) : "FULL_TIME")
                            .remote(job.remote())
                            .source("ARBEITNOW")
                            .companyName(job.company_name())
                            .applyUrl(job.url())
                            .postedAt(job.created_at() != null
                                ? LocalDate.ofEpochDay(job.created_at() / 86400) : LocalDate.now())
                            .collectedAt(LocalDateTime.now())
                            .skills(skills)
                            .build();

                        jobOfferRepo.save(jobOffer);
                        total++;
                    } catch (Exception e) {
                        log.debug("Erreur offre Arbeitnow {}: {}", job.slug(), e.getMessage());
                    }
                }

                Thread.sleep(300);

            } catch (Exception e) {
                log.error("❌ Erreur page {} Arbeitnow", page, e);
                break;
            }
        }

        log.info("✅ Collecte Arbeitnow terminée — {} offres", total);

        // Recalcule les agrégats mensuels après la collecte (règle métier)
        trendService.calculateAndSaveTrends();
    }

    // Records mapping JSON Arbeitnow
    record ArbeitnowResponse(List<ArbeitnowJob> data, Map<String, Object> links) {}
    record ArbeitnowJob(
        String slug, String company_name, String title, String description,
        String location, Boolean remote, List<String> job_types, String url,
        List<String> tags, Long created_at
    ) {}
}
