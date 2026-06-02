# Documentation Technique — SkillMap

## 1. Architecture des couches

```
┌─────────────────────────────────────┐
│  COLLECTEURS (scheduled, toutes les heures)
│  FranceTravailCollector             │  → OAuth2 → France Travail API
│  ArbeitnowCollector                 │  → Public → Arbeitnow API
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│  PARSER                             │
│  SkillParser.extractSkills(text)    │  → Dictionnaire 50+ technos
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│  PERSISTENCE (JPA + PostgreSQL)     │
│  JobOfferRepository                 │
│  SkillRepository                    │
│  SkillTrendRepository               │
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│  SERVICES (logique métier)          │
│  SkillService    → top skills       │
│  TrendService    → évolutions       │
│  InsightService  → analyse IA       │
│  StatsService    → stats globales   │
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│  CONTROLLER (REST API)              │
│  GET /api/skills                    │
│  GET /api/trends                    │
│  GET /api/cities                    │
│  GET /api/insights  (Redis cache)   │
│  GET /api/stats                     │
└─────────────────────────────────────┘
```

## 2. Modèle de données

```sql
job_offers          -- Offres collectées brutes
  └── job_offer_skills -- Relation N:N
skills              -- Compétences normalisées
skill_trends        -- Agrégats mensuels (calculés)
```

## 3. Virtual Threads — Java 21

Chaque requête HTTP s'exécute dans un Virtual Thread.
Configuration dans ThreadConfig.java :
  Executors.newVirtualThreadPerTaskExecutor()

Avantage : le backend gère des milliers de requêtes concurrentes
sans créer autant de threads OS. Idéal pour les I/O intensives
(appels DB, appels API externes).

## 4. Cache Redis

| Cache       | TTL    | Contenu                        |
|-------------|--------|--------------------------------|
| insights    | 1h     | Insights IA générés            |
| topSkills   | 30min  | Top 20 compétences du mois     |
| cityStats   | 30min  | Répartition géographique       |
| globalStats | 15min  | Statistiques header dashboard  |

## 5. Sécurité

Rate Limiting : Redis key "rate_limit:{IP}" → incrémenté à chaque requête,
expire après 1 minute. Si > 100 → HTTP 429.

JWT : Spring Security 6 oauth2ResourceServer, tokens validés via jwks-uri.

CORS : seulement localhost:4200 (dev) et les domaines Firebase (staging/prod).

## 6. CI/CD Flow

```
Push develop
    │
    ├── test (JUnit 5 + Testcontainers)
    ├── build (Docker multi-stage → Artifact Registry)
    ├── infra (Terraform staging.tfvars)
    ├── configure (Ansible playbook)
    └── smoke (curl /actuator/health)

Merge main (+ approbation GitHub)
    │
    ├── test
    ├── build
    ├── deploy-prod (Terraform prod.tfvars)
    ├── configure (Ansible prod)
    └── verify + notify Slack
```

## 7. Repositories à implémenter

```java
// JobOfferRepository
boolean existsByExternalId(String externalId);
List<Object[]> countBySkillNameGroupByCity(String skillName);
List<Object[]> countSkillsByMonth(String yearMonth);

// SkillTrendRepository
List<SkillTrend> findBySkillNameAndYearMonthBetween(...);
List<SkillTrend> findTopByYearMonthOrderByMentionCountDesc(...);

// SkillRepository
Optional<Skill> findByName(String name);
List<Skill> findByCategory(String category);
```

## 8. Services à implémenter

### SkillService
- getTopSkills(limit, month, category) → List<SkillDTO>
- getCityDistribution(skill, limit) → List<CityDTO>

### TrendService
- getMonthlyTrend(skill, months) → List<TrendDTO>
- compareSkills(skills, months) → List<TrendDTO>
- calculateAndSaveTrends() → appelé après chaque collecte

### InsightService
- getInsights() → List<InsightDTO>
- Logique : détecter les skills avec growthRate > 20%, corrélations, alertes

### StatsService
- getGlobalStats() → StatsDTO
- totalJobsAnalyzed, totalSkillsTracked, lastUpdated, etc.
