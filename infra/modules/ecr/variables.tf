variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "service_names" {
  description = "List of microservice names to create repositories for"
  type        = list(string)
  default     = [
    "trade-ingestion-service",
    "activity-monitor-service",
    "alert-service",
    "case-management-service"
  ]
}