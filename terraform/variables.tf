variable "project_id" {
  type = string
}

variable "region" {
  type    = string
  default = "europe-west1"
}

variable "env" {
  type = string # staging | prod
}

variable "image_tag" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true
  default   = ""
}

variable "redis_host" {
  type    = string
  default = "localhost"
}

# Secrets France Travail — passés via -var ou TF_VAR_ft_client_id / TF_VAR_ft_client_secret
variable "ft_client_id" {
  type      = string
  sensitive = true
  default   = ""
}

variable "ft_client_secret" {
  type      = string
  sensitive = true
  default   = ""
}

# Domaine frontend autorisé en CORS (Firebase Hosting)
# Site Firebase par défaut = <project-id>.web.app
variable "frontend_url" {
  type    = string
  default = "https://skillmap-498218.web.app"
}
