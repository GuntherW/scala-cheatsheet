terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
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
  default     = "scala-native-hello"
}

variable "lambda_zip_path" {
  description = "Path to the deployment ZIP (must contain 'bootstrap' binary)"
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

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# ─────────────────────────────────────────────
# Lambda Function (Custom Runtime = provided.al2023)
# Die ZIP-Datei muss eine ausführbare Datei namens 'bootstrap' enthalten
# ─────────────────────────────────────────────

resource "aws_lambda_function" "scala_native" {
  function_name = var.function_name
  description   = "Scala 3 / Scala Native Lambda function (Custom Runtime provided.al2023)"

  filename         = var.lambda_zip_path
  source_code_hash = filebase64sha256(var.lambda_zip_path)

  # Custom Runtime: handler-Wert wird ignoriert, bootstrap Binary übernimmt
  handler = "bootstrap"
  runtime = "provided.al2023"
  architectures = ["x86_64"]

  role = aws_iam_role.lambda_role.arn

  memory_size = 128   # Native Binary braucht deutlich weniger als JVM
  timeout     = 30

  environment {
    variables = {
      NATIVE_BINARY = "true"
    }
  }
}

# ─────────────────────────────────────────────
# Lambda Function URL (kein API Gateway nötig)
# ─────────────────────────────────────────────

resource "aws_lambda_function_url" "scala_native_url" {
  function_name      = aws_lambda_function.scala_native.function_name
  authorization_type = "NONE"
}

resource "aws_lambda_permission" "function_url_public" {
  statement_id           = "FunctionURLAllowPublicAccess"
  action                 = "lambda:InvokeFunctionUrl"
  function_name          = aws_lambda_function.scala_native.function_name
  principal              = "*"
  function_url_auth_type = "NONE"
}

resource "aws_lambda_permission" "invoke_public" {
  statement_id  = "InvokeAllowPublicAccess"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.scala_native.function_name
  principal     = "*"
}

resource "aws_cloudwatch_log_group" "lambda_logs" {
  name              = "/aws/lambda/${var.function_name}"
  retention_in_days = 7
}

# ─────────────────────────────────────────────
# Outputs
# ─────────────────────────────────────────────

output "function_name" {
  value = aws_lambda_function.scala_native.function_name
}

output "function_url" {
  value = aws_lambda_function_url.scala_native_url.function_url
}

output "function_arn" {
  value = aws_lambda_function.scala_native.arn
}
