$pw = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String((kubectl get secret db-secret -o jsonpath="{.data.password}")))
kubectl run psql --rm -i --restart=Never --image=postgres:16 --env="PGPASSWORD=$pw" -- psql -h surveillance-prod-db.ccfkou6gmo5z.us-east-1.rds.amazonaws.com -U surveillance -d surveillance -c "SELECT pg_stat_statements_reset();"
$nlb = kubectl get svc trade-ingestion-service -o jsonpath="{.status.loadBalancer.ingress[0].hostname}"
k6 run --env BASE_URL=http://$nlb .\scripts\load\trades.js