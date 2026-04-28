variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "bootstrap_brokers" {
  description = "MSK Kafka bootstrap brokers"
  type        = string
}