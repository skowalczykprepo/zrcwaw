variable "aws_region" {
  description = "Region AWS (Learner Lab obsluguje us-east-1 i us-west-2)"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefiks nazw zasobow"
  type        = string
  default     = "zrcwaw-chat"
}

variable "image_tag" {
  description = "Tag obrazow Dockera w ECR, ktory maja uruchamiac serwisy ECS"
  type        = string
  default     = "latest"
}

variable "db_username" {
  description = "Uzytkownik bazy RDS"
  type        = string
  default     = "chat"
}

variable "db_password" {
  description = "Haslo bazy RDS (min. 8 znakow) - podaj w terraform.tfvars, nie commituj"
  type        = string
  sensitive   = true
}

variable "alert_email" {
  description = "Adres e-mail, na ktory CloudWatch (przez SNS) wysyla powiadomienia o alarmach"
  type        = string
}

variable "response_time_threshold_seconds" {
  description = "Prog alarmu: sredni czas odpowiedzi backendu (sekundy)"
  type        = number
  default     = 2
}
