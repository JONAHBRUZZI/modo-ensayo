output "alb_dns_name" {
  description = "ALB DNS name for backend API"
  value       = aws_lb.main.dns_name
}

output "backend_ecr_repository" {
  description = "ECR repository URL for backend images"
  value       = aws_ecr_repository.backend.repository_url
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.main.endpoint
}

output "frontend_cloudfront_url" {
  description = "CloudFront distribution URL for frontend"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "frontend_s3_bucket" {
  description = "S3 bucket name for frontend static files"
  value       = aws_s3_bucket.frontend.id
}
