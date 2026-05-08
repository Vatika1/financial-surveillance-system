output "bootstrap_brokers" {
  description = "Plaintext Kafka bootstrap brokers"
  value       = aws_msk_cluster.main.bootstrap_brokers
}

output "bootstrap_brokers_tls" {
  description = "TLS Kafka bootstrap brokers"
  value       = aws_msk_cluster.main.bootstrap_brokers_tls
}

output "bootstrap_brokers_secret_arn" {
  description = "ARN of the Secrets Manager secret containing bootstrap brokers"
  value       = aws_secretsmanager_secret.msk_bootstrap_brokers.arn
}

output "bootstrap_brokers_secret_name" {
  description = "Name of the Secrets Manager secret containing bootstrap brokers"
  value       = aws_secretsmanager_secret.msk_bootstrap_brokers.name
}