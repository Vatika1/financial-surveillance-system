resource "aws_secretsmanager_secret" "db_connection" {
  name = "${var.project_name}-${var.environment}/db-connection"

  tags = {
    Name = "${var.project_name}-${var.environment}-db-connection"
  }
}

resource "aws_secretsmanager_secret_version" "db_connection" {
  secret_id = aws_secretsmanager_secret.db_connection.id

  secret_string = jsonencode({
    endpoint = var.db_endpoint
    dbname   = var.db_name
    username = var.db_username
  })
}

resource "aws_secretsmanager_secret" "kafka_connection" {
  name = "${var.project_name}-${var.environment}/kafka-connection"

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