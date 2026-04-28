output "kafka_connection_secret_arn" {
  description = "ARN of the Kafka connection secret"
  value       = aws_secretsmanager_secret.kafka_connection.arn
}