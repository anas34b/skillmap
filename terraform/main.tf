terraform {
  required_providers {
    google = { source = "hashicorp/google", version = "~> 5.0" }
  }
  # État stocké dans GCS — JAMAIS en local sur Git
  backend "gcs" {
    bucket = "skillmap-tfstate"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Numéro de projet → service account par défaut de Cloud Run (compute)
data "google_project" "this" {}

locals {
  # SA d'exécution par défaut des services Cloud Run
  run_sa = "${data.google_project.this.number}-compute@developer.gserviceaccount.com"
}

# ── Artifact Registry ─────────────────────────────────────────
resource "google_artifact_registry_repository" "docker" {
  repository_id = "skillmap"
  format        = "DOCKER"
  location      = var.region
  description   = "SkillMap Docker images"
}

# ── Cloud SQL PostgreSQL ───────────────────────────────────────
resource "google_sql_database_instance" "postgres" {
  name             = "skillmap-db-${var.env}"
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier = var.env == "prod" ? "db-g1-small" : "db-f1-micro"

    backup_configuration {
      enabled            = var.env == "prod"
      start_time         = "03:00"
      binary_log_enabled = false
    }

    maintenance_window {
      day  = 7 # Dimanche
      hour = 4
    }

    ip_configuration {
      ipv4_enabled = true
    }
  }

  deletion_protection = var.env == "prod"
}

resource "google_sql_database" "skillmap" {
  name     = "skillmap"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "skillmap" {
  name     = "skillmap"
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
}

# ── Secrets (Secret Manager) ──────────────────────────────────
# Créés ici pour que Cloud Run puisse les référencer dès le 1ᵉʳ apply
# (évite le poule/œuf avec Ansible). Valeurs passées via -var / TF_VAR_*.
resource "google_secret_manager_secret" "db_password" {
  secret_id = "DB_PASSWORD_${upper(var.env)}"
  replication {
    auto {}
  }
}
resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.db_password
}

resource "google_secret_manager_secret" "ft_client_id" {
  secret_id = "FT_CLIENT_ID_${upper(var.env)}"
  replication {
    auto {}
  }
}
resource "google_secret_manager_secret_version" "ft_client_id" {
  count       = var.ft_client_id == "" ? 0 : 1
  secret      = google_secret_manager_secret.ft_client_id.id
  secret_data = var.ft_client_id
}

resource "google_secret_manager_secret" "ft_client_secret" {
  secret_id = "FT_CLIENT_SECRET_${upper(var.env)}"
  replication {
    auto {}
  }
}
resource "google_secret_manager_secret_version" "ft_client_secret" {
  count       = var.ft_client_secret == "" ? 0 : 1
  secret      = google_secret_manager_secret.ft_client_secret.id
  secret_data = var.ft_client_secret
}

# ── IAM pour le SA d'exécution Cloud Run ──────────────────────
resource "google_project_iam_member" "run_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${local.run_sa}"
}

resource "google_project_iam_member" "run_secret_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${local.run_sa}"
}

# ── Cloud Run — Backend Spring Boot ───────────────────────────
resource "google_cloud_run_v2_service" "backend" {
  name     = "skillmap-backend-${var.env}"
  location = var.region

  template {
    # Connexion Cloud SQL (socket factory côté JVM via l'API connector)
    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = [google_sql_database_instance.postgres.connection_name]
      }
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/skillmap/backend:${var.image_tag}"

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.env
      }
      # Connexion DB via le Cloud SQL Java Socket Factory
      env {
        name  = "DB_URL"
        value = "jdbc:postgresql:///skillmap?cloudSqlInstance=${google_sql_database_instance.postgres.connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
      }
      env {
        name  = "DB_USER"
        value = google_sql_user.skillmap.name
      }
      env {
        name  = "REDIS_HOST"
        value = var.redis_host
      }
      # Émetteur OIDC public (les endpoints du dashboard sont publics ; JWT non requis)
      env {
        name  = "JWT_ISSUER_URI"
        value = "https://accounts.google.com"
      }
      env {
        name  = "FRONTEND_URL_STAGING"
        value = var.frontend_url
      }

      # Secrets depuis Secret Manager
      env {
        name = "DB_PASS"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }
      env {
        name = "FT_CLIENT_ID"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.ft_client_id.secret_id
            version = "latest"
          }
        }
      }
      env {
        name = "FT_CLIENT_SECRET"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.ft_client_secret.secret_id
            version = "latest"
          }
        }
      }

      resources {
        limits = {
          cpu    = var.env == "prod" ? "2" : "1"
          memory = var.env == "prod" ? "1Gi" : "512Mi"
        }
      }

      startup_probe {
        http_get { path = "/actuator/health" }
        initial_delay_seconds = 20
        period_seconds        = 10
        failure_threshold     = 12
      }

      liveness_probe {
        http_get { path = "/actuator/health" }
        initial_delay_seconds = 30
        period_seconds        = 30
      }
    }

    scaling {
      min_instance_count = var.env == "prod" ? 1 : 0
      max_instance_count = var.env == "prod" ? 10 : 2
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [
    google_secret_manager_secret_version.db_password,
    google_project_iam_member.run_sql_client,
    google_project_iam_member.run_secret_accessor,
  ]
}

# Accès public en lecture (dashboard public)
resource "google_cloud_run_service_iam_member" "public" {
  location = var.region
  service  = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# ── Outputs ───────────────────────────────────────────────────
output "backend_url" {
  value       = google_cloud_run_v2_service.backend.uri
  description = "URL du backend Cloud Run"
}

output "cloudsql_connection_name" {
  value       = google_sql_database_instance.postgres.connection_name
  description = "Nom de connexion Cloud SQL (PROJECT:REGION:INSTANCE)"
}
