output "bootstrap_brokers" {
  description = "Plaintext Kafka bootstrap brokers"
  value       = aws_msk_cluster.main.bootstrap_brokers
}

output "bootstrap_brokers_tls" {
  description = "TLS Kafka bootstrap brokers"
  value       = aws_msk_cluster.main.bootstrap_brokers_tls
}