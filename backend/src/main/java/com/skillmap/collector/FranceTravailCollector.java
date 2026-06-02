package com.skillmap.collector;

import com.skillmap.entity.JobOffer;
import com.skillmap.entity.Skill;
import com.skillmap.parser.SkillParser;
import com.skillmap.repository.JobOfferRepository;
import com.skillmap.repository.SkillRepository;
import com.skillmap.service.TrendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class FranceTravailCollector {

    private static final String TOKEN_URL =
        "https://entreprise.francetravail.fr/connexion/oauth2/access_token?realm=%2Fpartenaire";
    private static final String SEARCH_URL =
        "https://api.francetravail.io/partenaire/offresdemploi/v2/offres/search";

    private final RestClient restClient;
    private final SkillParser skillParser;
    private final JobOfferRepository jobOfferRepo;
    private final SkillRepository skillRepo;
    private final TrendService trendService;

    @Value("${FT_CLIENT_ID:}")
    private String clientId;

    @Value("${FT_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${skillmap.collector.keywords}")
    private List<String> keywords;

    public FranceTravailCollector(RestClient.Builder builder,
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

    // Planifié : toutes les heures (configurable via application.yml)
    @Scheduled(cron = "${skillmap.collector.cron}")
    public void collect() {
        log.info("▶ Démarrage collecte France Travail — {}", LocalDateTime.now());

        if (clientId.isBlank() || clientSecret.isBlank()) {
            log.warn("⚠️ FT_CLIENT_ID ou FT_CLIENT_SECRET non configurés — collecte ignorée");
            return;
        }

        try {
            String token = getAccessToken();
            int total = 0;

            for (String keyword : keywords) {
                int saved = fetchAndSave(token, keyword);
                total += saved;
                log.info("  ✓ '{}' → {} offres sauvegardées", keyword, saved);

                // Petite pause pour respecter le rate limit de l'API
                Thread.sleep(500);
            }

            log.info("✅ Collecte France Travail terminée — {} offres au total", total);

            // Recalcule les agrégats mensuels après la collecte (règle métier)
            trendService.calculateAndSaveTrends();

        } catch (Exception e) {
            log.error("❌ Erreur collecte France Travail", e);
        }
    }

    // ─── Token OAuth2 ─────────────────────────────────────────────────────────
    private String getAccessToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", "api_offresdemploiv2 o2dsoffre");

        var response = restClient.post()
            .uri(TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        return (String) Objects.requireNonNull(response).get("access_token");
    }

    // ─── Fetch + Save ─────────────────────────────────────────────────────────
    private int fetchAndSave(String token, String keyword) {
        var response = restClient.get()
            .uri(SEARCH_URL + "?motsCles={kw}&range=0-49&typeContrat=CDI,CDD,MIS", keyword)
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .body(FranceTravailResponse.class);

        if (response == null || response.resultats() == null) return 0;

        int saved = 0;
        for (var offre : response.resultats()) {
            try {
                // Évite les doublons via externalId
                if (jobOfferRepo.existsByExternalId(offre.id())) continue;

                // Extrait les compétences
                String description = buildDescription(offre);
                List<String> skillNames = skillParser.extractSkills(description);
                List<Skill> skills = resolveSkills(skillNames);

                JobOffer job = JobOffer.builder()
                    .externalId(offre.id())
                    .title(offre.intitule())
                    .description(description)
                    .city(extractCity(offre))
                    .department(offre.lieuTravail() != null ? offre.lieuTravail().libelle() : null)
                    .contractType(offre.typeContratLibelle())
                    .source("FRANCE_TRAVAIL")
                    .companyName(offre.entreprise() != null ? offre.entreprise().nom() : null)
                    .applyUrl(offre.origineOffre() != null ? offre.origineOffre().urlOrigine() : null)
                    .postedAt(parseDate(offre.dateCreation()))
                    .collectedAt(LocalDateTime.now())
                    .skills(skills)
                    .build();

                jobOfferRepo.save(job);
                saved++;
            } catch (Exception e) {
                log.debug("Erreur traitement offre {}: {}", offre.id(), e.getMessage());
            }
        }
        return saved;
    }

    private List<Skill> resolveSkills(List<String> skillNames) {
        return skillNames.stream()
            .map(name -> skillRepo.findByName(name)
                .orElseGet(() -> skillRepo.save(
                    Skill.builder()
                        .name(name)
                        .category(skillParser.getCategory(name))
                        .build()
                )))
            .toList();
    }

    private String buildDescription(FranceTravailOffre offre) {
        StringBuilder sb = new StringBuilder();
        if (offre.intitule() != null)    sb.append(offre.intitule()).append(" ");
        if (offre.description() != null) sb.append(offre.description()).append(" ");
        if (offre.competences() != null) {
            offre.competences().forEach(c -> sb.append(c.libelle()).append(" "));
        }
        return sb.toString();
    }

    private String extractCity(FranceTravailOffre offre) {
        if (offre.lieuTravail() == null) return null;
        String libelle = offre.lieuTravail().libelle();
        if (libelle == null) return null;
        // "75 - Paris" → "Paris"
        return libelle.contains(" - ") ? libelle.split(" - ")[1].trim() : libelle;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return LocalDate.now();
        try { return LocalDate.parse(dateStr.substring(0, 10)); }
        catch (Exception e) { return LocalDate.now(); }
    }

    // ─── Records pour le mapping JSON France Travail ───────────────────────
    record FranceTravailResponse(List<FranceTravailOffre> resultats, Integer totalResultats) {}
    record FranceTravailOffre(
        String id, String intitule, String description, String typeContratLibelle,
        String dateCreation, LieuTravail lieuTravail, Entreprise entreprise,
        OrigineOffre origineOffre, List<Competence> competences
    ) {}
    record LieuTravail(String libelle, String codePostal, String commune) {}
    record Entreprise(String nom, String description) {}
    record OrigineOffre(String urlOrigine) {}
    record Competence(String libelle) {}
}
