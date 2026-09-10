function grafana { kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80 }
function prom    { kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090 }
function ti      { kubectl port-forward deploy/trade-ingestion-service 8081:8081 }
function pods    { kubectl get pods; kubectl get pods -n monitoring }
function sclogs  { kubectl logs -n monitoring deploy/monitoring-grafana -c grafana-sc-dashboard | Select-String "trade-surveillance" }
function images  { kubectl get pods -o custom-columns=NAME:.metadata.name,IMAGE:.spec.containers[0].image }