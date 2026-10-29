# Buduje obrazy backendu i frontendu i wypycha je do ECR.
# Uruchom z katalogu projekt-1/terraform po `terraform apply` (repozytoria ECR musza istniec).
#   .\push-images.ps1            -> tag "latest"
#   .\push-images.ps1 -Tag v2    -> inny tag (wtedy ustaw image_tag = "v2" w terraform.tfvars)

param([string]$Tag = "latest")

$ErrorActionPreference = "Stop"

$region    = "us-east-1"
$accountId = (aws sts get-caller-identity --query Account --output text)
$registry  = "$accountId.dkr.ecr.$region.amazonaws.com"

Write-Host "Logowanie do ECR: $registry"
aws ecr get-login-password --region $region | docker login --username AWS --password-stdin $registry

$projectDir = Resolve-Path (Join-Path $PSScriptRoot "..")

foreach ($module in @("backend", "frontend")) {
    $image = "$registry/zrcwaw-chat-${module}:$Tag"
    Write-Host "Buduje $module -> $image"
    docker build --platform linux/amd64 -t $image (Join-Path $projectDir $module)
    docker push $image
}

Write-Host "Gotowe. Jesli serwisy ECS juz dzialaja, wymus nowe wdrozenie:"
Write-Host "  aws ecs update-service --cluster zrcwaw-chat-cluster --service zrcwaw-chat-backend  --force-new-deployment"
Write-Host "  aws ecs update-service --cluster zrcwaw-chat-cluster --service zrcwaw-chat-frontend --force-new-deployment"
