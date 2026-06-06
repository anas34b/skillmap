# 🗺️ SkillMap — Le radar du marché tech en France

[![Staging](https://github.com/anas34b/skillmap/actions/workflows/staging.yml/badge.svg?branch=develop)](https://github.com/anas34b/skillmap/actions/workflows/staging.yml)
[![Java](https://img.shields.io/badge/Java-21-blue?logo=java)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?logo=spring)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17-red?logo=angular)](https://angular.io)
[![GCP](https://img.shields.io/badge/GCP-Cloud%20Run-orange?logo=google-cloud)](https://cloud.google.com/run)

> Analyse en temps réel des offres d'emploi tech françaises :  
> quelles compétences sont les plus demandées, dans quelles villes, et comment les tendances évoluent.

🔗 **Dashboard (staging)** → https://skillmap-498218.web.app  
📊 **API (staging)** → https://skillmap-backend-staging-r2soyhq3ua-ew.a.run.app

---

## 🚀 Stack

| Couche | Technologie | Pourquoi |
|--------|------------|----------|
| Backend | Java 21 + Spring Boot 3.3 | Virtual Threads, Records, Pattern Matching |
| Frontend | Angular 17 + Tailwind CSS | Signals, Standalone Components, dark theme + Chart.js |
| Base de données | PostgreSQL 16 (Cloud SQL) | Robuste, bien supporté sur GCP |
| Cache | Redis 7 | Rate limiting + cache insights (fail-open) |
| Backend hosting | GCP Cloud Run | Serverless, scale to zero |
| Frontend hosting | Firebase Hosting | CDN statique pour le SPA Angular |
| IaC | Terraform 1.9 | Infrastructure as Code, state dans GCS |
| Config | Ansible | Post-config Cloud Run (IAM, smoke test) |
| CI/CD | GitHub Actions | Pipeline staging auto (push `develop`) |
| Auth CI → GCP | Workload Identity Federation | **Sans clé** (OIDC), org policy interdit les clés SA |
| Sécurité | Spring Security 6 + OWASP | JWT, HTTPS, CORS strict, Rate Limiting |

---

## 📊 Sources de données

| Source | Volume | Accès |
|--------|--------|-------|
| [France Travail API](https://francetravail.io/data/api/offres-emploi) | 700k+ offres officielles | OAuth2 gratuit |
| [Arbeitnow](https://www.arbeitnow.com/api) | Offres Europe tech | Public, sans clé |

---

## 🏗️ Architecture

```
France Travail API ──┐
                     ├──▶ Collectors (cron) ──▶ SkillParser ──▶ PostgreSQL (Cloud SQL)
Arbeitnow API ───────┘                                              │
                                                                    ▼
            Dashboard Angular ◀── REST /api ◀── Services ◀── Repositories JPA
            (Firebase Hosting)     (Cloud Run)
```

---

## 🛠️ Lancer en local

### Prérequis
- Java 21+, Docker & Docker Compose, Node 20+
- (Optionnel) Compte France Travail pour les vraies données

```bash
# 1. Clone
git clone https://github.com/anas34b/skillmap.git
cd skillmap

# 2. Variables d'environnement (optionnel pour France Travail)
cp .env.example .env        # éditer FT_CLIENT_ID / FT_CLIENT_SECRET

# 3. Dépendances locales
docker compose up -d postgres redis

# 4. Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
#   → http://localhost:8080  (utiliser --server.port=8081 si 8080 est pris)

# 5. Frontend (autre terminal)
cd frontend && npm install && ng serve
#   → http://localhost:4200
```

> En dev, le frontend pointe sur `http://localhost:8081/api` (cf. `frontend/src/environments/environment.ts`).
> En prod, `environment.production.ts` pointe sur l'URL Cloud Run.

---

## 🌿 Git Flow

```
main     ──▶ Production  (approbation manuelle)
develop  ──▶ Staging     (déploiement automatique : backend + frontend)
feature/ ──▶ Pull Request vers develop
```

---

## ☁️ Déploiement (CI/CD automatisé)

Chaque **push sur `develop`** déclenche le workflow `staging.yml` (6 jobs) :

```
test → build (Docker → Artifact Registry) → infra (Terraform)
     → configure (Ansible) → smoke (health check)
     └─ frontend (ng build → Firebase Hosting)   [en parallèle]
```

L'authentification CI → GCP se fait **sans clé de service account** via **Workload Identity Federation**
(une org policy interdit `iam.disableServiceAccountKeyCreation`).

### Secrets GitHub requis
| Secret | Rôle |
|--------|------|
| `GCP_PROJECT_ID` | ID du projet GCP |
| `GCP_WIF_PROVIDER` | Provider Workload Identity (chemin complet) |
| `GCP_SA_EMAIL` | Email du Service Account `github-ci@…` |
| `FT_CLIENT_ID` / `FT_CLIENT_SECRET` | Identifiants France Travail |

📖 **Procédure complète de mise en place** (projet GCP, WIF, Firebase, secrets, infra) :
voir **[docs/DEPLOIEMENT.md](docs/DEPLOIEMENT.md)**.

---

## 🔐 Sécurité

- ✅ JWT stateless (Spring Security 6 OAuth2 Resource Server)
- ✅ HTTPS (Cloud Run + Firebase Hosting)
- ✅ CORS strict (domaines Firebase autorisés via `FRONTEND_URL_STAGING`)
- ✅ Rate Limiting : 100 req/min par IP (Redis, fail-open)
- ✅ Secrets dans GCP Secret Manager (jamais dans le code)
- ✅ CI sans clé (Workload Identity Federation)
- ✅ Docker : utilisateur non-root

---

## 🧪 Tests

```bash
cd backend
./mvnw test                 # JUnit 5 + Mockito (+ Testcontainers pour l'intégration)
```

---

## 📡 API Endpoints

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/skills?limit=20&month=2026-06&category=LANGUAGE` | Top compétences du mois |
| GET | `/api/trends?skill=Java&months=6` | Évolution mensuelle |
| GET | `/api/cities?skill=Angular&limit=10` | Répartition géographique |
| GET | `/api/insights` | Analyse IA (cache 1h) |
| GET | `/api/stats` | Statistiques globales |
| GET | `/api/skills/compare?skills=Java,Python` | Comparaison compétences |
| GET | `/actuator/health` | Health check |

---

## 👤 Auteur

**Anas Daoui** — [GitHub](https://github.com/anas34b)

---

_Projet full-stack moderne : Java 21 (Virtual Threads), Spring Boot 3, Angular 17,
GCP (Cloud Run + Cloud SQL + Firebase), Terraform, Ansible, GitHub Actions CI/CD (WIF)._
