# Déploiement — SkillMap (staging GCP)

Procédure réelle de mise en production sur GCP, et journal des points d'attention rencontrés.

## Vue d'ensemble

| Élément | Service GCP | Détail |
|---|---|---|
| Backend | **Cloud Run** | `skillmap-backend-staging` (europe-west1), public, scale 0→2 |
| Base de données | **Cloud SQL** PostgreSQL 16 | `skillmap-db-staging`, accès via Cloud SQL Socket Factory |
| Image Docker | **Artifact Registry** | dépôt `skillmap` (europe-west1) |
| Secrets | **Secret Manager** | `DB_PASSWORD_STAGING`, `FT_CLIENT_ID_STAGING`, `FT_CLIENT_SECRET_STAGING` (créés par Terraform) |
| Frontend | **Firebase Hosting** | `https://skillmap-498218.web.app` |
| État Terraform | **GCS** | bucket `skillmap-tfstate` |
| Auth CI → GCP | **Workload Identity Federation** | sans clé (OIDC GitHub) |

Projet : `skillmap-498218` (numéro `172845189101`) · Région : `europe-west1`

---

## 1. Setup unique (une seule fois, dans Cloud Shell)

```bash
PROJECT_ID=skillmap-498218
gcloud config set project $PROJECT_ID

# APIs
gcloud services enable run.googleapis.com sqladmin.googleapis.com \
  artifactregistry.googleapis.com secretmanager.googleapis.com \
  iamcredentials.googleapis.com iam.googleapis.com cloudresourcemanager.googleapis.com \
  firebase.googleapis.com firebasehosting.googleapis.com

# Service Account de la CI + rôles
gcloud iam service-accounts create github-ci --display-name="GitHub Actions CI"
SA=github-ci@$PROJECT_ID.iam.gserviceaccount.com
for ROLE in roles/run.admin roles/cloudsql.admin roles/artifactregistry.admin \
            roles/secretmanager.admin roles/iam.serviceAccountUser roles/storage.admin \
            roles/resourcemanager.projectIamAdmin roles/firebasehosting.admin; do
  gcloud projects add-iam-policy-binding $PROJECT_ID --member="serviceAccount:$SA" --role="$ROLE" --condition=None
done

# Bucket du tfstate
gcloud storage buckets create gs://skillmap-tfstate --location=europe-west1

# Workload Identity Federation (auth sans clé pour GitHub Actions)
gcloud iam workload-identity-pools create github-pool --location=global --display-name="GitHub Actions"
gcloud iam workload-identity-pools providers create-oidc github-provider \
  --location=global --workload-identity-pool=github-pool \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='anas34b/skillmap'"
gcloud iam service-accounts add-iam-policy-binding $SA \
  --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/172845189101/locations/global/workloadIdentityPools/github-pool/attribute.repository/anas34b/skillmap"

# Firebase : ajouter Firebase au projet via la console (console.firebase.google.com →
# "Ajouter Firebase au projet Google Cloud" → skillmap-498218). La CLI addfirebase
# échoue en 403 tant que l'onboarding/CGU n'est pas fait dans la console.
```

### Secrets GitHub (Settings → Secrets and variables → Actions)
| Secret | Valeur |
|---|---|
| `GCP_PROJECT_ID` | `skillmap-498218` |
| `GCP_WIF_PROVIDER` | `projects/172845189101/locations/global/workloadIdentityPools/github-pool/providers/github-provider` |
| `GCP_SA_EMAIL` | `github-ci@skillmap-498218.iam.gserviceaccount.com` |
| `FT_CLIENT_ID` / `FT_CLIENT_SECRET` | identifiants France Travail |

---

## 2. CI/CD — `.github/workflows/staging.yml`

Déclenché à chaque **push sur `develop`** :

1. **test** — `mvnw test`
2. **build** — crée le dépôt Artifact Registry (idempotent) puis build & push de l'image Docker
3. **infra** — `terraform apply` (Cloud SQL, secrets, IAM, Cloud Run)
4. **configure** — Ansible (grant IAM Secret Manager + smoke)
5. **smoke** — health check `/actuator/health`
6. **frontend** — `ng build` + `firebase deploy --only hosting` (en parallèle, `needs: test`)

Auth GCP de chaque job via `google-github-actions/auth@v2` + WIF (`workload_identity_provider` + `service_account`), avec `permissions: id-token: write`.

---

## 3. Points d'attention (résolus)

- **Clés de Service Account interdites** par l'org policy `iam.disableServiceAccountKeyCreation` → on utilise **Workload Identity Federation** (sans clé) partout (CI + Firebase).
- **API `iamcredentials.googleapis.com`** requise pour l'impersonation WIF (sinon 403 à l'auth).
- **Poule/œuf Artifact Registry** : Cloud Run référence l'image → le dépôt est créé par le job `build` **avant** le push (et retiré de Terraform).
- **Secrets gérés par Terraform** (et non plus Ansible) pour exister avant le déploiement Cloud Run ; valeurs passées via `-var`. Le mot de passe DB est généré (`random_password`) et stocké en Secret Manager.
- **Connexion Cloud SQL** : dépendance `com.google.cloud.sql:postgres-socket-factory` + `DB_URL=jdbc:postgresql:///skillmap?cloudSqlInstance=...&socketFactory=...` + rôle `cloudsql.client` sur le SA d'exécution.
- **Health check Redis exclu** (`management.health.redis.enabled=false`) : pas de Memorystore en staging, sinon `/actuator/health` = DOWN → la sonde Cloud Run échoue.
- **CORS** : `FRONTEND_URL_STAGING` = `https://skillmap-498218.web.app` (site Firebase par défaut = `<project-id>.web.app`).
- **Cold start** : `min_instance_count = 0` en staging → la 1ʳᵉ requête après inactivité prend ~50 s. Le frontend gère ça avec un **écran de chargement + retry auto**. Pour zéro attente : passer `min_instance_count = 1`.

---

## 4. Commandes utiles

```bash
# URL du backend
gcloud run services describe skillmap-backend-staging --region=europe-west1 --format="value(status.url)"

# Logs du backend (démarrage / erreurs)
gcloud logging read \
  'resource.type="cloud_run_revision" resource.labels.service_name="skillmap-backend-staging"' \
  --project=skillmap-498218 --freshness=1h --limit=80 --order=asc --format="value(textPayload)"

# Re-déployer : push sur develop (ou Actions → Re-run jobs)
git push origin develop
```
