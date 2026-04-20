variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where MSK will be created"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for MSK brokers"
  type        = list(string)
}

variable "eks_node_security_group_id" {
  description = "Security group ID of EKS worker nodes"
  type        = string
}

variable "instance_type" {
  description = "MSK broker instance type"
  type        = string
  default     = "kafka.t3.small"
}

variable "kafka_version" {
  description = "Apache Kafka version"
  type        = string
  default     = "3.5.1"
}

variable "ebs_volume_size" {
  description = "EBS volume size per broker in GB"
  type        = number
  default     = 20
}