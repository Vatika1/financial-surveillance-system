resource "aws_secretsmanager_secret" "kafka_connection" {
  name                    = "${var.project_name}-${var.environment}/kafka-connection"
  recovery_window_in_days = 0

  tags = {
    Name = "${var.project_name}-${var.environment}-kafka-connection"
  }
}

resource "aws_secretsmanager_secret_version" "kafka_connection" {
  secret_id = aws_secretsmanager_secret.kafka_connection.id

  secret_string = jsonencode({
    bootstrap_brokers = var.bootstrap_brokers
  })
}