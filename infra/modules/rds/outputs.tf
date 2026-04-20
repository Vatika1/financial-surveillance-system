output "db_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.main.endpoint
}

output "db_name" {
  description = "Database name"
  value       = aws_db_instance.main.db_name
}

output "db_username" {
  description = "Database master username"
  value       = aws_db_instance.main.username
}

output "db_password_secret_arn" {
  description = "ARN of the secret containing the database password"
  value       = aws_db_instance.main.master_user_secret[0].secret_arn
}