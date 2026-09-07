resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${var.project_name}-${var.environment}-nat-eip"
  }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = data.terraform_remote_state.persistent.outputs.public_subnet_ids[0]

  tags = {
    Name = "${var.project_name}-${var.environment}-nat"
  }
}

resource "aws_route" "private_nat" {
  route_table_id         = data.terraform_remote_state.persistent.outputs.private_route_table_id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.main.id
}
