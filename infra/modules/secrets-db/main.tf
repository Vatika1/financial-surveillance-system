resource "aws_secretsmanager_secret" "db_connection" {
  name                    = "${var.project_name}-${var.environment}/db-connection"
  recovery_window_in_days = 0

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