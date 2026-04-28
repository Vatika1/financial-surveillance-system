module "vpc" {
  source = "../../../modules/vpc"

  project_name = var.project_name
  environment  = var.environment
}

module "ecr" {
  source = "../../../modules/ecr"

  project_name = var.project_name
  environment  = var.environment
}

module "rds" {
  source = "../../../modules/rds"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
}

module "secrets_db" {
  source = "../../../modules/secrets-db"

  project_name = var.project_name
  environment  = var.environment
  db_endpoint  = module.rds.db_endpoint
  db_name      = module.rds.db_name
  db_username  = module.rds.db_username
}