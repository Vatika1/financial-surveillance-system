data "terraform_remote_state" "persistent" {
  backend = "s3"

  config = {
    bucket = "vatika-surveillance-tfstate-7981"
    key    = "surveillance/prod/persistent/terraform.tfstate"
    region = "us-east-1"
  }
}