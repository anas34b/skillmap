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

# ── Cloud Run — Backend Spring Boot ───────────────────────────
resource "google_cloud_run_v2_service" "backend" {
  name     = "skillmap-backend-${var.env}"
  location = var.region

  template {
    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/skillmap/backend:${var.image_tag}"

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = var.env
      }
      env {
        name  = "REDIS_HOST"
        value = var.redis_host
      }

      # Secrets depuis Secret Manager
      env {
        name = "DB_PASS"
        value_source {
          secret_key_ref {
            secret  = "DB_PASSWORD_${upper(var.env)}"
            version = "latest"
          }
        }
      }
      env {
        name = "FT_CLIENT_SECRET"
        value_source {
          secret_key_ref {
            secret  = "FT_CLIENT_SECRET_${upper(var.env)}"
            version = "latest"
          }
        }
      }

      resources {
        limits = {
          cpu    = var.env == "prod" ? "2" : "1"
          memory = var.env == "prod" ? "512Mi" : "256Mi"
        }
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
