module "eks" {
  source = "../../../modules/eks"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = data.terraform_remote_state.persistent.outputs.vpc_id
  private_subnet_ids = data.terraform_remote_state.persistent.outputs.private_subnet_ids
}

module "msk" {
  source = "../../../modules/msk"

  project_name               = var.project_name
  environment                = var.environment
  vpc_id                     = data.terraform_remote_state.persistent.outputs.vpc_id
  private_subnet_ids         = data.terraform_remote_state.persistent.outputs.private_subnet_ids
  eks_node_security_group_id = module.eks.node_security_group_id
}

module "secrets_kafka" {
  source = "../../../modules/secrets-kafka"

  project_name      = var.project_name
  environment       = var.environment
  bootstrap_brokers = module.msk.bootstrap_brokers
}

# Standalone ingress rule on RDS security group, allowing EKS nodes to connect
# Lives here (in ephemeral) because it's about EKS access, not RDS structure
resource "aws_security_group_rule" "rds_from_eks" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = module.eks.node_security_group_id
  security_group_id        = data.terraform_remote_state.persistent.outputs.rds_security_group_id
  description              = "PostgreSQL from EKS nodes"
}