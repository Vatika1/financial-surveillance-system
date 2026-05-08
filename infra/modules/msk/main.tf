# Security group for MSK
resource "aws_security_group" "msk" {
  name        = "${var.project_name}-${var.environment}-msk-sg"
  description = "Security group for MSK Kafka"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Kafka from EKS nodes"
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [var.eks_node_security_group_id]
  }

  ingress {
    description     = "Kafka TLS from EKS nodes"
    from_port       = 9094
    to_port         = 9094
    protocol        = "tcp"
    security_groups = [var.eks_node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-msk-sg"
  }
}

# MSK Cluster
resource "aws_msk_cluster" "main" {
  cluster_name           = "${var.project_name}-${var.environment}"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = 2

  broker_node_group_info {
    instance_type   = var.instance_type
    client_subnets  = var.private_subnet_ids
    security_groups = [aws_security_group.msk.id]

    storage_info {
      ebs_storage_info {
        volume_size = var.ebs_volume_size
      }
    }
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-msk"
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.msk_broker_logs.name
      }
    }
  }
}

resource "aws_cloudwatch_log_group" "msk_broker_logs" {
  name              = "/aws/msk/${var.project_name}-${var.environment}"
  retention_in_days = 7

  tags = {
    Name = "${var.project_name}-${var.environment}-msk-logs"
  }
}

# Bootstrap brokers stored in Secrets Manager
# Updated automatically every time the MSK cluster is recreated
resource "aws_secretsmanager_secret" "msk_bootstrap_brokers" {
  name                    = "${var.project_name}-${var.environment}-msk-bootstrap-brokers"
  description             = "TLS bootstrap broker connection string for MSK cluster"
  recovery_window_in_days = 0  # immediate deletion on destroy (ephemeral stack)

  tags = {
    Name = "${var.project_name}-${var.environment}-msk-bootstrap-brokers"
  }
}

resource "aws_secretsmanager_secret_version" "msk_bootstrap_brokers" {
  secret_id     = aws_secretsmanager_secret.msk_bootstrap_brokers.id
  secret_string = aws_msk_cluster.main.bootstrap_brokers_tls
}