terraform {
  backend "s3" {
    bucket         = "vatika-surveillance-tfstate-7981"
    key            = "surveillance/prod/ephemeral/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
  }
}