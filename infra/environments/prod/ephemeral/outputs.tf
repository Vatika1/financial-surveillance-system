output "grafana_role_arn" {
  value = module.eks.grafana_role_arn
}

output "redis_endpoint" {
  value = module.elasticache.redis_endpoint
}