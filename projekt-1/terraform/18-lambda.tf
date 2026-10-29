# AWS Lambda (punkt 11): funkcja uruchamiana automatycznie po wgraniu pliku do S3.

data "archive_file" "media_processor" {
  type        = "zip"
  source_file = "${path.module}/lambda/handler.py"
  output_path = "${path.module}/media_processor.zip"
}

resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${var.project_name}-media-processor"
  retention_in_days = 7
}

resource "aws_lambda_function" "media_processor" {
  function_name    = "${var.project_name}-media-processor"
  role             = data.aws_iam_role.lab.arn
  runtime          = "python3.12"
  handler          = "handler.handler"
  filename         = data.archive_file.media_processor.output_path
  source_code_hash = data.archive_file.media_processor.output_base64sha256
  timeout          = 30

  environment {
    variables = {
      SNS_TOPIC_ARN = aws_sns_topic.alerts.arn
    }
  }

  depends_on = [aws_cloudwatch_log_group.lambda]
}

# S3 musi miec prawo wywolac Lambde...
resource "aws_lambda_permission" "allow_s3" {
  statement_id  = "AllowExecutionFromS3"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.media_processor.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.media.arn
}

# ...a bucket wysyla zdarzenie po kazdym nowym obiekcie.
resource "aws_s3_bucket_notification" "media_uploaded" {
  bucket = aws_s3_bucket.media.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.media_processor.arn
    events              = ["s3:ObjectCreated:*"]
  }

  depends_on = [aws_lambda_permission.allow_s3]
}
