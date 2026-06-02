# SkillMap — Instructions pour Claude Code

## 🎯 Contexte du projet
SkillMap est un dashboard qui analyse en temps réel les offres d'emploi tech
françaises pour révéler quelles compétences sont les plus demandées, dans
quelles villes, et comment les tendances évoluent mois après mois.

**Stack :**
- Backend  : Java 21 + Spring Boot 3.3.x
- Frontend : Angular 17+
- Cloud    : GCP Cloud Run (backend) + Firebase Hosting (frontend)
- IaC      : Terraform + Ansible
- CI/CD    : GitHub Actions (staging → prod)
- DB       : PostgreSQL 16 + Redis 7

---

## 📁 Structure du projet
```
skillmap/
├── backend/          # Spring Boot 3 + Java 21
├── frontend/         # Angular 17+
├── terraform/        # Infrastructure GCP
├── ansible/          # Configuration & secrets
├── .github/workflows # CI/CD pipelines
├── docs/             # Documentation métier et technique
└── CLAUDE.md         # Ce fichier
```

---

## 🔑 Sources de données (APIs)

### 1. France Travail API (principale)
- Doc officielle : https://francetravail.io/data/api/offres-emploi
- Auth : OAuth2 client_credentials
- Base URL : https://api.francetravail.io/partenaire/offresdemploi/v2
- Endpoint clé : GET /offres/search?motsCles=Java&range=0-49
- Variables d'env : FT_CLIENT_ID, FT_CLIENT_SECRET
- Scope requis : api_offresdemploiv2 o2dsoffre

### 2. Arbeitnow API (secondaire)
- Doc officielle : https://www.arbeitnow.com/api
- Auth : aucune (publique)
- Base URL : https://www.arbeitnow.com/api/job-board-api
- Endpoint : GET ?page=1
- Pas de clé API requise

---

## ⚙️ Variables d'environnement requises
```
# France Travail
FT_CLIENT_ID=xxx
FT_CLIENT_SECRET=xxx

# Base de données
DB_URL=jdbc:postgresql://localhost:5432/skillmap
DB_USER=skillmap
DB_PASS=xxx

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# GCP (pour déploiement)
GCP_PROJECT_ID=xxx
GCP_REGION=europe-west1
```

---

## 🏗️ Architecture backend

### Couches applicatives
```
collector/    → Appels APIs externes (FranceTravailCollector, ArbeitnowCollector)
parser/       → Extraction compétences des descriptions (SkillParser)
entity/       → JPA entities (JobOffer, Skill, SkillTrend)
repository/   → Spring Data JPA repositories
service/      → Logique métier (SkillService, TrendService, InsightService)
controller/   → REST endpoints Angular (SkillMapController)
security/     → JWT + Rate Limiting (SecurityConfig, RateLimitFilter)
config/       → Beans Spring (ThreadConfig, RedisConfig, SchedulerConfig)
dto/          → DTOs réponses API (SkillDTO, TrendDTO, CityDTO, InsightDTO)
```

### Endpoints REST à implémenter
```
GET /api/skills?limit=20&month=2026-06    → Top compétences du mois
GET /api/trends?skill=Java                → Évolution mensuelle
GET /api/cities?skill=Angular             → Répartition géographique
GET /api/insights                         → Analyse IA (mis en cache Redis 1h)
GET /api/stats                            → Statistiques globales
GET /actuator/health                      → Health check GCP
```

---

## 🔐 Sécurité (OWASP)
- JWT stateless via Spring Security 6 oauth2ResourceServer
- HTTPS forcé (requiresSecure)
- CORS strict : seulement les domaines Firebase autorisés
- Rate Limiting : 100 req/min par IP via Redis
- Validation @Valid sur tous les @RequestBody
- Secrets dans GCP Secret Manager (jamais dans le code)
- Docker : utilisateur non-root

---

## 🌿 Convention Git
```
main     → production (déploiement avec approbation manuelle)
develop  → staging (déploiement automatique)
feature/ → nouvelles fonctionnalités
fix/     → corrections de bugs
```

---

## 📋 Règles de code Java

### Toujours utiliser :
- Records Java 21 pour les DTOs (immuables, boilerplate réduit)
- Virtual Threads (configuré dans ThreadConfig.java)
- Injection par constructeur (jamais @Autowired sur les champs)
- @Transactional sur les méthodes de service qui écrivent en DB
- @Cacheable(value="insights") sur les méthodes coûteuses
- Optional<T> pour éviter les NullPointerExceptions
- Streams Java pour les transformations de collections

### Exemple DTO correct :
```java
// ✅ Record Java 21
public record SkillDTO(String name, int count, double growthRate, String trend) {}

// ❌ À éviter
public class SkillDTO {
    private String name;
    // getters/setters...
}
```

### Exemple injection correcte :
```java
// ✅ Injection par constructeur
@Service
public class SkillService {
    private final SkillRepository repo;
    public SkillService(SkillRepository repo) { this.repo = repo; }
}

// ❌ À éviter
@Service
public class SkillService {
    @Autowired
    private SkillRepository repo;
}
```

---

## 🧪 Tests
- Tests unitaires : JUnit 5 + Mockito
- Tests d'intégration : @SpringBootTest + Testcontainers (PostgreSQL + Redis)
- Couverture minimale : 70%
- Lancer les tests : ./mvnw test

---

## 🐳 Docker
- Multi-stage build (JDK pour build, JRE pour runtime)
- Image base : eclipse-temurin:21-jre-alpine
- Utilisateur non-root obligatoire
- Port exposé : 8080

---

## ☁️ Déploiement GCP
- Backend : Cloud Run (europe-west1)
- Frontend : Firebase Hosting
- DB : Cloud SQL PostgreSQL 16
- Cache : Memorystore Redis
- Images Docker : Artifact Registry
- Secrets : Secret Manager

---

## 🚀 Commandes utiles
```bash
# Lancer le backend en local
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Lancer avec Docker Compose
docker-compose up -d

# Build Docker
docker build -t skillmap-backend ./backend

# Terraform staging
cd terraform && terraform apply -var-file=staging.tfvars

# Ansible secrets
cd ansible && ansible-playbook playbook.yml -e env=staging

# Tests
cd backend && ./mvnw test
```
