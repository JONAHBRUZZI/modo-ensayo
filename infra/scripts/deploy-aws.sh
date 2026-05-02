#!/usr/bin/env bash
set -euo pipefail

ENVIRONMENT="${1:-dev}"
REGION="${AWS_REGION:-us-east-1}"

echo "=== Modo Ensayo - AWS Deploy ==="
echo "Environment: $ENVIRONMENT"
echo "Region: $REGION"

cd "$(dirname "$0")/../terraform"

terraform init -upgrade

terraform plan -var="environment=$ENVIRONMENT" -var="aws_region=$REGION" -out=tfplan

echo "Applying Terraform plan..."
terraform apply tfplan

echo ""
echo "=== Deployment Complete ==="
echo "Run the following to see outputs:"
echo "  terraform output"
