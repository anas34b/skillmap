variable "project_id"   { type = string }
variable "region"       { type = string, default = "europe-west1" }
variable "env"          { type = string }  # staging | prod
variable "image_tag"    { type = string }
variable "db_password"  { type = string, sensitive = true, default = "" }
variable "redis_host"   { type = string, default = "localhost" }
