output "db_connection_secret_arn" {
  description = "ARN of the database connection secret"
  value       = aws_secretsmanager_secret.db_connection.arn
}

output "kafka_connection_secret_arn" {
  description = "ARN of the Kafka connection secret"
  value       = aws_secretsmanager_secret.kafka_connection.arn
}