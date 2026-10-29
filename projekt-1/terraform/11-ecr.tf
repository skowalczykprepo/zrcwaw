# Repozytoria obrazow Dockera - po jednym na modul.

resource "aws_ecr_repository" "backend" {
  name         = "${var.project_name}-backend"
  force_delete = true # pozwala na terraform destroy mimo obrazow w repozytorium

  image_scanning_configuration {
    scan_on_push = false
  }
}

resource "aws_ecr_repository" "frontend" {
  name         = "${var.project_name}-frontend"
  force_delete = true

  image_scanning_configuration {
    scan_on_push = false
  }
}
