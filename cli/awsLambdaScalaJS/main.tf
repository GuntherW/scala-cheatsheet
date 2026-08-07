terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.0"
    }
  }
}

provider "aws" {
  # Region via env var AWS_DEFAULT_REGION oder explizit:
  region = var.aws_region
  # Credentials werden automatisch aus ~/.aws/credentials geladen
}

# ─────────────────────────────────────────────
# Variables
# ─────────────────────────────────────────────

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-central-1"
}

variable "function_name" {
  description = "Name of the Lambda function"
  type        = string
  default     = "scala-js-hello"
}

variable "lambda_zip_path" {
  description = "Path to the deployment ZIP"
  type        = string
  default     = "./lambda.zip"
}

# ─────────────────────────────────────────────
# IAM Role für Lambda
# ─────────────────────────────────────────────

data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
    actions = ["sts:AssumeRole"]
  }
}

resource "aws_iam_role" "lambda_role" {
  name               = "${var.function_name}-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

# Basis-Policy für CloudWatch Logs
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# ─────────────────────────────────────────────
# Lambda Function
# ─────────────────────────────────────────────

resource "aws_lambda_function" "scala_js" {
  function_name = var.function_name
  description   = "Scala 3 / Scala.js Lambda function (Node.js runtime)"

  filename         = var.lambda_zip_path
  source_code_hash = filebase64sha256(var.lambda_zip_path)

  # handler = <filename ohne .js>.<export name>
  handler = "handler.handler"
  runtime = "nodejs22.x"

  role = aws_iam_role.lambda_role.arn

  memory_size = 256
  timeout     = 30

  environment {
    variables = {
      NODE_ENV = "production"
    }
  }
}

# ─────────────────────────────────────────────
# Lambda Function URL (kein API Gateway nötig)
# ─────────────────────────────────────────────

resource "aws_lambda_function_url" "scala_js_url" {
  function_name      = aws_lambda_function.scala_js.function_name
  authorization_type = "NONE" # Öffentlich erreichbar - für Produktion auf AWS_IAM setzen
}

# Erlaubt öffentliche Aufrufe über die Function URL
resource "aws_lambda_permission" "function_url_public" {
  statement_id           = "FunctionURLAllowPublicAccess"
  action                 = "lambda:InvokeFunctionUrl"
  function_name          = aws_lambda_function.scala_js.function_name
  principal              = "*"
  function_url_auth_type = "NONE"
}

# Zusätzlich benötigt laut AWS Console für public access
resource "aws_lambda_permission" "invoke_public" {
  statement_id  = "InvokeAllowPublicAccess"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.scala_js.function_name
  principal     = "*"
}

# CloudWatch Log Group (explizit, für saubere Löschung via terraform destroy)
resource "aws_cloudwatch_log_group" "lambda_logs" {
  name              = "/aws/lambda/${var.function_name}"
  retention_in_days = 7
}

# ─────────────────────────────────────────────
# Outputs
# ─────────────────────────────────────────────

output "function_name" {
  description = "Lambda function name"
  value       = aws_lambda_function.scala_js.function_name
}

output "function_url" {
  description = "Public URL of the Lambda function"
  value       = aws_lambda_function_url.scala_js_url.function_url
}

output "function_arn" {
  description = "Lambda function ARN"
  value       = aws_lambda_function.scala_js.arn
}
