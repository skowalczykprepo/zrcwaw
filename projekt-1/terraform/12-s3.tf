# Bucket na pliki multimedialne (punkt 7).

resource "aws_s3_bucket" "media" {
  # nazwa bucketa musi byc globalnie unikalna - doklejamy ID konta
  bucket        = "${var.project_name}-media-${data.aws_caller_identity.current.account_id}"
  force_destroy = true # terraform destroy usunie bucket razem z zawartoscia
}

resource "aws_s3_bucket_public_access_block" "media" {
  bucket                  = aws_s3_bucket.media.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
