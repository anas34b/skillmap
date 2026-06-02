# 🗺️ SkillMap — Le radar du marché tech en France

[![Staging](https://github.com/YOUR_USERNAME/skillmap/actions/workflows/staging.yml/badge.svg)](https://github.com/YOUR_USERNAME/skillmap/actions/workflows/staging.yml)
[![Production](https://github.com/YOUR_USERNAME/skillmap/actions/workflows/prod.yml/badge.svg)](https://github.com/YOUR_USERNAME/skillmap/actions/workflows/prod.yml)
[![Java](https://img.shields.io/badge/Java-21-blue?logo=java)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?logo=spring)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-17-red?logo=angular)](https://angular.io)
[![GCP](https://img.shields.io/badge/GCP-Cloud%20Run-orange?logo=google-cloud)](https://cloud.google.com/run)

> Analyse en temps réel des offres d'emploi tech françaises :  
> quelles compétences sont les plus demandées, dans quelles villes, et comment les tendances évoluent.

🔗 **Demo** → https://skillmap.web.app  
📊 **API** → https://skillmap-backend-prod-xxx.run.app

---

## 📸 Screenshots

_(ajoute tes screenshots ici une fois le projet buildé)_

---

## 🚀 Stack

| Couche | Technologie | Pourquoi |
|--------|------------|----------|
| Backend | Java 21 + Spring Boot 3.3 | Virtual Threads, Records, Pattern Matching |
| Frontend | Angular 17+ | Signals, Standalone Components |
| Base de données | PostgreSQL 16 | Robuste, bien supporté sur GCP |
| Cache | Redis 7 | Rate limiting + cache insights IA |
| Cloud | GCP Cloud Run | Serverless, scale to zero |
| IaC | Terraform 1.7+ | Infrastructure as Code, multi-env |
| Config | Ansible 2.16+ | Gestion secrets GCP Secret Manager |
| CI/CD | GitHub Actions | Pipeline 5 étapes, staging auto + prod approuvé |
| Sécurité | Spring Security 6 + OWASP | JWT, HTTPS, CORS, Rate Limiting |

---

## 📊 Sources de données

| Source | Volume | Accès |
|--------|--------|-------|
| [France Travail API](https://francetravail.io/data/api/offres-emploi) | 700k+ offres officielles | OAuth2 gratuit |
| [Arbeitnow](https://www.arbeitnow.com/api) | Offres Europe tech | Public, sans clé |

> **Pourquoi ces sources ?** LinkedIn Jobs, Indeed, Glassdoor, WTTJ et APEC
> ne proposent pas d'API publique accessible aux développeurs indépendants.
> France Travail est la source officielle gouvernementale française.

---

## 🏗️ Architecture

```
France Travail API ──┐
                     ├──▶ FranceTravailCollector  ┐
Arbeitnow API ───────┘    ArbeitnowCollector      ├──▶ SkillParser
                                                   ┘         │
                                                              ▼
                                                        PostgreSQL
                                                              │
                          Angular Dashboard ◀── REST API ◀───┘
                          (Firebase Hosting)   (Cloud Run)
```

---

## 🛠️ Lancer en local

### Prérequis
- Java 21+
- Docker & Docker Compose
- (Optionnel) Compte France Travail pour les vraies données

```bash
# 1. Clone
git clone https://github.com/YOUR_USERNAME/skillmap.git
cd skillmap

# 2. Variables d'environnement (optionnel pour France Travail)
cp .env.example .env
# Éditer .env avec tes FT_CLIENT_ID et FT_CLIENT_SECRET

# 3. Lancer PostgreSQL + Redis
docker-compose up -d postgres redis

# 4. Lancer le backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 5. Lancer le frontend (dans un autre terminal)
cd frontend
npm install
ng serve

# 6. Ouvrir http://localhost:4200
```

### Avec Docker Compose complet
```bash
docker-compose up -d
# Backend : http://localhost:8080
# Frontend : http://localhost:4200
```

---

## 🌿 Git Flow

```
main     ──▶ Production  (approbation manuelle requise)
develop  ──▶ Staging     (déploiement automatique)
feature/ ──▶ Pull Request vers develop
```

---

## 🔐 Sécurité

- ✅ JWT stateless (Spring Security 6 OAuth2 Resource Server)
- ✅ HTTPS forcé sur tous les endpoints
- ✅ CORS strict : seulement les domaines Firebase autorisés
- ✅ Rate Limiting : 100 req/min par IP (Redis)
- ✅ Validation @Valid sur tous les inputs
- ✅ SQL Injection impossible (JPA/Hibernate)
- ✅ Secrets dans GCP Secret Manager (jamais dans le code)
- ✅ Docker : utilisateur non-root

---

## ☁️ Déploiement GCP

### Prérequis GCP
```bash
# 1. Créer le projet GCP
gcloud projects create skillmap-project

# 2. Activer les APIs
gcloud services enable run.googleapis.com \
  sqladmin.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com

# 3. Créer le bucket Terraform state
gsutil mb gs://skillmap-tfstate

# 4. Créer un Service Account pour GitHub Actions
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions"
```

### Secrets GitHub (Settings → Secrets)
```
GCP_PROJECT_ID     → ton project ID GCP
GCP_SA_KEY         → JSON du Service Account
FT_CLIENT_ID       → Client ID France Travail
FT_CLIENT_SECRET   → Client Secret France Travail
SLACK_WEBHOOK      → URL webhook Slack (optionnel)
```

### Déployer
```bash
# Push sur develop → staging automatique
git push origin develop

# Merge sur main → prod (approbation requise dans GitHub)
git checkout main && git merge develop && git push
```

---

## 🧪 Tests

```bash
cd backend

# Tests unitaires
./mvnw test

# Tests avec coverage
./mvnw test jacoco:report

# Rapport : target/site/jacoco/index.html
```

---

## 📡 API Endpoints

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/skills?limit=20&month=2026-06` | Top compétences du mois |
| GET | `/api/trends?skill=Java&months=6` | Évolution mensuelle |
| GET | `/api/cities?skill=Angular&limit=10` | Répartition géographique |
| GET | `/api/insights` | Analyse IA (cache 1h) |
| GET | `/api/stats` | Statistiques globales |
| GET | `/api/skills/compare?skills=Java,Python` | Comparaison compétences |
| GET | `/actuator/health` | Health check |

---

## 👤 Auteur

**Ton Nom** — [LinkedIn](https://linkedin.com/in/tonprofil) · [GitHub](https://github.com/tonusername)

---

_Projet réalisé pour démontrer une architecture full-stack moderne :  
Java 21 (Virtual Threads), Spring Boot 3, Angular 17, GCP, Terraform, Ansible, GitHub Actions CI/CD._
