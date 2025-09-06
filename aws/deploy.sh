#!/bin/bash
set -euo pipefail

# AWS CI/CD Deployment Script for Store App
# This script deploys the production-ready Docker image to AWS infrastructure

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
AWS_REGION=${AWS_REGION:-"us-east-1"}
ENVIRONMENT=${ENVIRONMENT:-"production"}
STACK_NAME="${ENVIRONMENT}-store-app"
ECR_REPOSITORY="store-app"

# Functions for colored output
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if AWS CLI is configured
check_aws_cli() {
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi

    if ! aws sts get-caller-identity &> /dev/null; then
        log_error "AWS CLI is not configured. Please run 'aws configure' first."
        exit 1
    fi

    local account_id=$(aws sts get-caller-identity --query 'Account' --output text)
    log_info "Using AWS Account: ${account_id}"
}

# Function to deploy CloudFormation stack
deploy_infrastructure() {
    log_info "Deploying infrastructure stack: ${STACK_NAME}"
    
    local template_path="aws/infrastructure/cloudformation-template.yml"
    
    if [[ ! -f "$template_path" ]]; then
        log_error "CloudFormation template not found: $template_path"
        exit 1
    fi

    aws cloudformation deploy \
        --template-file "$template_path" \
        --stack-name "$STACK_NAME" \
        --parameter-overrides \
            EnvironmentName="$ENVIRONMENT" \
        --capabilities CAPABILITY_NAMED_IAM \
        --region "$AWS_REGION" \
        --no-fail-on-empty-changeset

    if [[ $? -eq 0 ]]; then
        log_success "Infrastructure deployment completed"
    else
        log_error "Infrastructure deployment failed"
        exit 1
    fi
}

# Function to get stack outputs
get_stack_outputs() {
    log_info "Retrieving stack outputs"
    
    aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].Outputs' \
        --output table
}

# Function to create secrets in AWS Secrets Manager
create_secrets() {
    log_info "Creating application secrets"
    
    local db_endpoint=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
        --output text)

    local redis_endpoint=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`RedisEndpoint`].OutputValue' \
        --output text)

    # Create database secrets
    aws secretsmanager create-secret \
        --name "${ENVIRONMENT}-store-database" \
        --description "Store App Database Configuration for ${ENVIRONMENT}" \
        --secret-string "{
            \"url\": \"jdbc:postgresql://${db_endpoint}:5432/store_db\",
            \"username\": \"store_user\",
            \"password\": \"$(openssl rand -base64 32)\"
        }" \
        --region "$AWS_REGION" \
        --no-cli-pager || log_warning "Database secret already exists"

    # Create Redis auth token secret
    aws secretsmanager create-secret \
        --name "${ENVIRONMENT}-store-redis-auth" \
        --description "Store App Redis Auth Token for ${ENVIRONMENT}" \
        --secret-string "{
            \"password\": \"$(openssl rand -base64 32)\"
        }" \
        --region "$AWS_REGION" \
        --no-cli-pager || log_warning "Redis auth secret already exists"

    log_success "Secrets configuration completed"
}

# Function to build and push Docker image
build_and_push_image() {
    log_info "Building and pushing Docker image to ECR"
    
    local account_id=$(aws sts get-caller-identity --query 'Account' --output text)
    local registry="${account_id}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    local image_tag="latest"
    local full_image_name="${registry}/${ECR_REPOSITORY}:${image_tag}"

    # Login to ECR
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$registry"

    # Build the image
    log_info "Building Docker image..."
    docker build -t "$ECR_REPOSITORY" .

    # Tag the image
    docker tag "$ECR_REPOSITORY:latest" "$full_image_name"

    # Push the image
    log_info "Pushing image to ECR..."
    docker push "$full_image_name"

    log_success "Docker image pushed successfully: $full_image_name"
}

# Function to create or update ECS service
deploy_application() {
    log_info "Deploying application to ECS"

    local account_id=$(aws sts get-caller-identity --query 'Account' --output text)
    local cluster_name="${ENVIRONMENT}-store-cluster"
    local service_name="${ENVIRONMENT}-store-app-service"
    local task_definition_family="${ENVIRONMENT}-store-app-task"

    # Process task definition template
    local task_def_template="aws/task-definition-template.json"
    local task_def_processed="/tmp/task-definition-${ENVIRONMENT}.json"

    sed "s/\${AWS_ACCOUNT_ID}/${account_id}/g; s/\${AWS_REGION}/${AWS_REGION}/g; s/\${ENVIRONMENT}/${ENVIRONMENT}/g" \
        "$task_def_template" > "$task_def_processed"

    # Register task definition
    aws ecs register-task-definition \
        --cli-input-json "file://${task_def_processed}" \
        --region "$AWS_REGION"

    # Check if service exists
    if aws ecs describe-services \
        --cluster "$cluster_name" \
        --services "$service_name" \
        --region "$AWS_REGION" \
        --query 'services[0].serviceName' \
        --output text 2>/dev/null | grep -q "$service_name"; then
        
        log_info "Updating existing ECS service"
        aws ecs update-service \
            --cluster "$cluster_name" \
            --service "$service_name" \
            --task-definition "$task_definition_family" \
            --force-new-deployment \
            --region "$AWS_REGION"
    else
        log_info "Creating new ECS service"
        
        local target_group_arn=$(aws cloudformation describe-stacks \
            --stack-name "$STACK_NAME" \
            --region "$AWS_REGION" \
            --query 'Stacks[0].Outputs[?OutputKey==`ALBTargetGroup`].OutputValue' \
            --output text)

        local vpc_id=$(aws cloudformation describe-stacks \
            --stack-name "$STACK_NAME" \
            --region "$AWS_REGION" \
            --query 'Stacks[0].Outputs[?OutputKey==`VPC`].OutputValue' \
            --output text)

        local subnet_ids=$(aws ec2 describe-subnets \
            --filters "Name=vpc-id,Values=${vpc_id}" "Name=tag:Name,Values=*Private*" \
            --query 'Subnets[].SubnetId' \
            --output text \
            --region "$AWS_REGION" | tr '\t' ',')

        local security_group_id=$(aws ec2 describe-security-groups \
            --filters "Name=vpc-id,Values=${vpc_id}" "Name=group-name,Values=${ENVIRONMENT}-ECS-SecurityGroup" \
            --query 'SecurityGroups[0].GroupId' \
            --output text \
            --region "$AWS_REGION")

        aws ecs create-service \
            --cluster "$cluster_name" \
            --service-name "$service_name" \
            --task-definition "$task_definition_family" \
            --desired-count 2 \
            --launch-type FARGATE \
            --network-configuration "awsvpcConfiguration={subnets=[$subnet_ids],securityGroups=[$security_group_id],assignPublicIp=DISABLED}" \
            --load-balancers "targetGroupArn=$target_group_arn,containerName=store-app,containerPort=8081" \
            --health-check-grace-period-seconds 60 \
            --enable-execute-command \
            --region "$AWS_REGION"
    fi

    log_success "ECS service deployment initiated"
}

# Function to wait for deployment to complete
wait_for_deployment() {
    log_info "Waiting for deployment to stabilize..."
    
    local cluster_name="${ENVIRONMENT}-store-cluster"
    local service_name="${ENVIRONMENT}-store-app-service"

    aws ecs wait services-stable \
        --cluster "$cluster_name" \
        --services "$service_name" \
        --region "$AWS_REGION"

    if [[ $? -eq 0 ]]; then
        log_success "Deployment completed successfully"
    else
        log_error "Deployment failed or timed out"
        exit 1
    fi
}

# Function to verify deployment health
verify_deployment() {
    log_info "Verifying deployment health"

    local load_balancer_url=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' \
        --output text)

    log_info "Load Balancer URL: $load_balancer_url"

    # Wait for load balancer to be ready
    sleep 30

    # Health check
    local health_check_url="${load_balancer_url}/actuator/health"
    local max_attempts=10
    local attempt=1

    while [[ $attempt -le $max_attempts ]]; do
        log_info "Health check attempt $attempt/$max_attempts"
        
        if curl -f -s "$health_check_url" > /dev/null; then
            log_success "Application is healthy!"
            curl -s "$health_check_url" | jq .
            break
        else
            if [[ $attempt -eq $max_attempts ]]; then
                log_error "Health check failed after $max_attempts attempts"
                exit 1
            fi
            log_warning "Health check failed, retrying in 30 seconds..."
            sleep 30
            ((attempt++))
        fi
    done
}

# Function to display deployment summary
display_summary() {
    log_success "Deployment Summary"
    echo "===================="
    echo "Environment: $ENVIRONMENT"
    echo "AWS Region: $AWS_REGION"
    echo "Stack Name: $STACK_NAME"
    echo ""
    
    local load_balancer_url=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$AWS_REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' \
        --output text)

    echo "🚀 Application URL: $load_balancer_url"
    echo "🏥 Health Check: ${load_balancer_url}/actuator/health"
    echo "📊 Metrics: ${load_balancer_url}/actuator/prometheus"
    echo ""
    echo "✅ Production-ready Docker image deployed successfully to AWS!"
}

# Main deployment function
main() {
    log_info "Starting AWS deployment for Store App"
    log_info "Environment: $ENVIRONMENT"
    log_info "Region: $AWS_REGION"

    # Deployment steps
    check_aws_cli
    deploy_infrastructure
    create_secrets
    build_and_push_image
    deploy_application
    wait_for_deployment
    verify_deployment
    display_summary

    log_success "🎉 Deployment completed successfully!"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --environment|-e)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --region|-r)
            AWS_REGION="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -e, --environment    Environment (staging/production) [default: production]"
            echo "  -r, --region         AWS Region [default: us-east-1]"
            echo "  -h, --help          Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Execute main function
main "$@"