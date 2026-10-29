# Zasoby, ktore JUZ istnieja w koncie Learner Lab i ktorych nie mozemy tworzyc sami.

# W Learner Lab nie wolno tworzyc rol IAM - uzywamy gotowej roli LabRole
# (dla ECS: execution role + task role, dla Lambdy: execution role).
data "aws_iam_role" "lab" {
  name = "LabRole"
}

# Domyslny VPC i jego publiczne podsieci - oszczedza budowania calej sieci od zera.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_caller_identity" "current" {}
