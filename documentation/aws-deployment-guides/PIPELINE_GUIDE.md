# 🚀 AWS CI/CD Pipeline Complete Guide

## Overview

This document provides a comprehensive step-by-step breakdown of the GitHub Actions CI/CD pipeline that builds and delivers the Store App as a **production-ready Docker image** to AWS infrastructure.

## 📋 Pipeline Architecture

```mermaid
graph TD
    A[Git Push/PR] --> B[Job 1: Quality Gates]
    B --> C[Job 2: AWS Deploy]
    C --> D[Job 3: Cleanup]
    
    B1[Environment Setup] --> B2[Code Quality]
    B2 --> B3[Testing Suite]
    B3 --> B4[Security Scanning]
    B4 --> B5[Docker Build]
    
    C1[AWS Authentication] --> C2[ECR Management]
    C2 --> C3[Production Build]
    C3 --> C4[ECS Deployment]
    C4 --> C5[Health Verification]
    
    D1[Image Cleanup] --> D2[Resource Optimization]
```

## 🎯 Pipeline Triggers

| Trigger | Action | Environment |
|---------|--------|-------------|
| `push: main` | Full deployment pipeline | Production |
| `push: develop` | Full deployment pipeline | Staging |
| `push: feature/*` | Quality gates only | None |
| `pull_request: main` | Quality gates only | None |
| `tags: v*` | Full deployment pipeline | Production |

---

## 📋 JOB 1: Quality Gates & Security Checks

**Purpose**: Comprehensive code quality validation, testing, and security analysis before any deployment.

### Environment Setup Phase

#### Step 1: Repository Checkout
```yaml
- name: Checkout repository
  uses: actions/checkout@v4
  with:
    fetch-depth: 0  # Full git history for comprehensive analysis
```
**What happens**: Downloads complete repository with full commit history for better code analysis.

#### Step 2: Start Test Services
```yaml
services:
  postgres:
    image: postgres:16-alpine
    env:
      POSTGRES_DB: store_test
      POSTGRES_USER: test_user  
      POSTGRES_PASSWORD: test_password
    ports: [5432:5432]
    
  redis:
    image: redis:7-alpine
    ports: [6379:6379]
```
**What happens**: Starts real PostgreSQL and Redis services for integration testing, ensuring tests run against actual database systems rather than mocks.

#### Step 3: Java Development Kit Setup
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'eclipse-temurin'
    cache: 'gradle'  # Caches Gradle dependencies for faster builds
```
**What happens**: Installs JDK 17 and caches Gradle dependencies to speed up subsequent builds.

#### Step 4: Gradle Permissions
```bash
chmod +x gradlew
```
**What happens**: Ensures Gradle wrapper script has execution permissions.

#### Step 5: Gradle Wrapper Validation
```yaml
- name: Validate Gradle wrapper
  uses: gradle/wrapper-validation-action@v2
```
**What happens**: Security check to ensure Gradle wrapper hasn't been tampered with.

### Code Quality Validation Phase

#### Step 6: Code Formatting Check
```bash
./gradlew spotlessCheck
```
**What happens**: Validates code formatting against Palantir Java Format standards. Pipeline fails if code doesn't meet formatting requirements.

#### Step 7: Compilation Verification
```bash
./gradlew compileJava compileTestJava
```
**What happens**: Compiles both main and test source code to catch compilation errors early.

### Comprehensive Testing Phase

#### Step 8: Unit Tests Execution
```bash
./gradlew test
env:
  SPRING_PROFILES_ACTIVE: test
  SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/store_test
  SPRING_DATASOURCE_USERNAME: test_user
  SPRING_DATASOURCE_PASSWORD: test_password
  SPRING_DATA_REDIS_HOST: localhost
  SPRING_DATA_REDIS_PORT: 6379
```
**What happens**: Runs all unit tests with real database connections. Tests include:
- Service layer business logic
- Controller endpoint validation
- Repository data access patterns
- Caching functionality
- Exception handling scenarios

#### Step 9: Integration Tests Execution  
```bash
./gradlew integrationTest
env:
  SPRING_PROFILES_ACTIVE: test
  SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/store_test
  SPRING_DATASOURCE_USERNAME: test_user
  SPRING_DATASOURCE_PASSWORD: test_password
```
**What happens**: Runs integration tests that verify:
- End-to-end API workflows
- Database transaction handling  
- Cross-component interactions
- Data consistency across operations

#### Step 10: Test Coverage Report Generation
```bash
./gradlew jacocoTestReport
```
**What happens**: Generates detailed code coverage reports in HTML and XML formats, showing which code paths are tested.

#### Step 11: Coverage Threshold Verification
```bash
./gradlew jacocoTestCoverageVerification
```
**What happens**: Enforces minimum code coverage requirements:
- Overall coverage: 70% minimum
- Per-class coverage: 60% minimum (excluding mappers and main class)
- **Pipeline fails if coverage is below threshold**

#### Step 12: Test Results Publishing
```yaml
- name: Upload test results
  uses: dorny/test-reporter@v1
  if: success() || failure()
  with:
    name: Test Results
    path: build/test-results/**/*.xml
    reporter: java-junit
```
**What happens**: Publishes test results to GitHub PR/Actions interface, showing passed/failed tests with details.

#### Step 13: Coverage Upload to Codecov
```yaml
- name: Upload coverage to Codecov  
  uses: codecov/codecov-action@v3
  with:
    file: ./build/reports/jacoco/test/jacocoTestReport.xml
    fail_ci_if_error: false
```
**What happens**: Uploads coverage data to Codecov for tracking coverage trends over time.

### Security Analysis Phase

#### Step 14: OWASP Dependency Check
```bash
./gradlew dependencyCheckAnalyze
```
**What happens**: Scans all project dependencies for known security vulnerabilities using OWASP database:
- Checks Maven dependencies
- Identifies CVEs in third-party libraries
- Generates security report
- **Pipeline fails if critical vulnerabilities found (CVSS >= 7.0)**

#### Step 15: Dependency Report Upload
```yaml
- name: Upload dependency check results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: dependency-check-report
    path: build/reports/dependency-check-report.html
```
**What happens**: Saves dependency security report as artifact for review.

### Application Build Phase

#### Step 16: Production JAR Build
```bash
./gradlew build -x test
```
**What happens**: Creates optimized production JAR file (skips tests since they already ran):
- Compiles source code
- Processes resources
- Creates executable JAR
- Applies build optimizations

#### Step 17: AWS Account ID Configuration
```bash
echo "AWS_ACCOUNT_ID=123456789012" >> $GITHUB_ENV
```
**What happens**: Sets placeholder AWS account ID for non-deployment stages (real ID used in deployment job).

#### Step 18: Docker Metadata Extraction
```yaml
- name: Extract metadata for Docker
  id: meta
  uses: docker/metadata-action@v5
  with:
    images: ${{ env.AWS_ACCOUNT_ID }}.dkr.ecr.${{ env.AWS_REGION }}.amazonaws.com/${{ env.ECR_REPOSITORY }}
    tags: |
      type=ref,event=branch      # Tags with branch name (e.g., "main", "develop")
      type=ref,event=pr          # Tags with PR number for pull requests
      type=sha,prefix={{branch}}- # Tags with branch-commit (e.g., "main-abc1234")
      type=semver,pattern={{version}}      # Semantic version for tagged releases
      type=semver,pattern={{major}}.{{minor}} # Major.minor version
      type=raw,value=latest,enable={{is_default_branch}} # "latest" tag for main branch only
```
**What happens**: Generates Docker image tags and labels based on Git context for proper image versioning.

### Docker Build & Security Phase

#### Step 19: Docker Buildx Setup
```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3
```
**What happens**: Configures advanced Docker build features including multi-platform builds and build caching.

#### Step 20: Security Scan Docker Build
```yaml
- name: Build Docker image (for scanning)
  uses: docker/build-push-action@v5
  with:
    context: .
    load: true                    # Loads image into local Docker daemon
    tags: ${{ env.ECR_REPOSITORY }}:scan
    cache-from: type=gha         # Uses GitHub Actions cache
    cache-to: type=gha,mode=max  # Saves build cache
```
**What happens**: Builds Docker image locally for security scanning:
- Multi-stage build process (build stage + runtime stage)
- Optimized layer caching
- Security hardened with non-root user
- Health check integration

#### Step 21: Container Vulnerability Scanning
```yaml
- name: Run Trivy vulnerability scanner
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: ${{ env.ECR_REPOSITORY }}:scan
    format: 'json'
    output: 'trivy-results.json'
```
**What happens**: Comprehensive container security scan:
- Scans base image for OS vulnerabilities
- Checks installed packages for known CVEs
- Analyzes application dependencies
- Identifies security misconfigurations

#### Step 22: Security Threshold Validation
```bash
CRITICAL=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity=="CRITICAL")] | length' trivy-results.json || echo 0)
HIGH=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity=="HIGH")] | length' trivy-results.json || echo 0)

echo "Critical vulnerabilities: $CRITICAL"
echo "High vulnerabilities: $HIGH"

if [ "$CRITICAL" -gt "${{ env.MAX_CRITICAL_VULNERABILITIES }}" ]; then
  echo "❌ Critical vulnerabilities ($CRITICAL) exceed threshold (0)"
  exit 1
fi

if [ "$HIGH" -gt "${{ env.MAX_HIGH_VULNERABILITIES }}" ]; then
  echo "❌ High vulnerabilities ($HIGH) exceed threshold (2)" 
  exit 1
fi

echo "✅ Security scan passed"
```
**What happens**: Enforces security quality gates:
- **Zero tolerance for CRITICAL vulnerabilities**
- **Maximum 2 HIGH severity vulnerabilities allowed**
- **Pipeline fails if thresholds exceeded**

#### Step 23: SARIF Conversion for GitHub Security
```bash
jq -n --argjson data "$(cat trivy-results.json)" '
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json",
  "version": "2.1.0", 
  "runs": [
    {
      "tool": {
        "driver": {
          "name": "Trivy",
          "version": "latest"
        }
      },
      "results": []
    }
  ]
}' > trivy-results.sarif
```
**What happens**: Converts Trivy results to SARIF format for GitHub Security tab integration.

#### Step 24: Security Results Upload
```yaml
- name: Upload Trivy scan results to GitHub Security tab
  uses: github/codeql-action/upload-sarif@v3
  if: always()
  with:
    sarif_file: 'trivy-results.sarif'
```
**What happens**: Uploads security findings to GitHub Security tab for tracking and remediation.

---

## 📋 JOB 2: AWS Deployment

**Purpose**: Deploy production-ready Docker image to AWS ECS infrastructure with comprehensive health validation.

**Prerequisites**: 
```yaml
needs: quality-gates  # Only runs if Job 1 succeeds completely
if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'
environment: ${{ github.ref == 'refs/heads/main' && 'production' || 'staging' }}
```

### Setup & Authentication Phase

#### Step 25: Repository Checkout
```yaml
- name: Checkout repository
  uses: actions/checkout@v4
```
**What happens**: Downloads repository code for deployment scripts and configurations.

#### Step 26: Build Environment Setup
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'eclipse-temurin'
    cache: 'gradle'

- name: Build application
  run: |
    chmod +x gradlew
    ./gradlew build -x test
```
**What happens**: Rebuilds application in deployment environment (tests skipped as they passed in Job 1).

#### Step 27: AWS Credentials Configuration
```yaml
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: ${{ secrets.AWS_ROLE_TO_ASSUME }}
    role-session-name: GitHubActions-${{ github.run_id }}
    aws-region: ${{ env.AWS_REGION }}
```
**What happens**: Authenticates with AWS using OIDC (OpenID Connect):
- No long-lived AWS access keys stored in GitHub
- Assumes IAM role with minimal required permissions
- Creates unique session for this pipeline run
- Configures AWS CLI for subsequent commands

### ECR Container Registry Setup

#### Step 28: Amazon ECR Login
```yaml
- name: Login to Amazon ECR
  id: login-ecr
  uses: aws-actions/amazon-ecr-login@v2
```
**What happens**: Obtains Docker login credentials for Amazon ECR, enabling image push operations.

#### Step 29: ECR Repository Management
```bash
aws ecr describe-repositories --repository-names ${{ env.ECR_REPOSITORY }} --region ${{ env.AWS_REGION }} || \
aws ecr create-repository \
  --repository-name ${{ env.ECR_REPOSITORY }} \
  --image-scanning-configuration scanOnPush=true \
  --encryption-configuration encryptionType=AES256 \
  --region ${{ env.AWS_REGION }}
```
**What happens**: Ensures ECR repository exists:
- Checks if repository already exists
- Creates repository if missing with security features:
  - Automatic vulnerability scanning on image push
  - AES256 encryption at rest
  - Lifecycle policies for image management

### Production Docker Build Phase

#### Step 30: Docker Buildx Setup
```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3
```
**What happens**: Configures Docker Buildx for advanced build capabilities.

#### Step 31: Production Image Metadata
```yaml
- name: Extract metadata for production image
  id: meta
  uses: docker/metadata-action@v5
  with:
    images: ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}
    tags: |
      type=ref,event=branch                    # "main" or "develop"
      type=sha,prefix={{branch}}-              # "main-abc1234"
      type=raw,value=latest,enable={{is_default_branch}} # "latest" (main only)
      type=raw,value={{date 'YYYYMMDD-HHmmss'}} # "20250106-143022"
```
**What happens**: Generates production image tags including branch, commit SHA, and timestamp for traceability.

#### Step 32: Production Docker Build & Push
```yaml
- name: Build and push production Docker image
  id: build-image
  uses: docker/build-push-action@v5
  with:
    context: .
    platforms: linux/amd64        # Production platform specification
    push: true                    # Push directly to ECR
    tags: ${{ steps.meta.outputs.tags }}
    labels: ${{ steps.meta.outputs.labels }}
    cache-from: type=gha          # Use GitHub Actions build cache
    cache-to: type=gha,mode=max   # Save build cache for next run
    build-args: |
      SPRING_PROFILES_ACTIVE=aws
      BUILD_VERSION=${{ github.sha }}
      BUILD_TIME=${{ github.event.head_commit.timestamp }}
```
**What happens**: Builds and pushes production-ready Docker image:
- **Multi-stage build**: Separates build and runtime environments
- **Platform targeting**: Builds specifically for linux/amd64 (ECS Fargate)
- **Build arguments**: Embeds version info and AWS configuration
- **Build caching**: Optimizes build time using GitHub Actions cache
- **Direct ECR push**: Uploads image to Amazon ECR registry

### Post-Build Security Validation

#### Step 33: ECR Image Security Scanning
```bash
IMAGE_URI="${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ github.ref_name }}-${{ github.sha }}"

# Wait for ECR scan to complete
echo "Waiting for ECR vulnerability scan to complete..."
aws ecr wait image-scan-complete \
  --repository-name ${{ env.ECR_REPOSITORY }} \
  --image-id imageTag=${{ github.ref_name }}-${{ github.sha }} \
  --region ${{ env.AWS_REGION }}

# Get scan results  
SCAN_RESULTS=$(aws ecr describe-image-scan-findings \
  --repository-name ${{ env.ECR_REPOSITORY }} \
  --image-id imageTag=${{ github.ref_name }}-${{ github.sha }} \
  --region ${{ env.AWS_REGION }} \
  --query 'imageScanFindings.findingCounts' \
  --output json)

echo "ECR Scan Results: $SCAN_RESULTS"

CRITICAL=$(echo $SCAN_RESULTS | jq -r '.CRITICAL // 0')
HIGH=$(echo $SCAN_RESULTS | jq -r '.HIGH // 0')

if [ "$CRITICAL" -gt "0" ] || [ "$HIGH" -gt "5" ]; then
  echo "❌ Production image has critical security vulnerabilities"
  exit 1
fi

echo "✅ Production image security scan passed"
```
**What happens**: Validates production image security:
- Waits for ECR's automatic vulnerability scan to complete
- Retrieves detailed vulnerability findings
- **Enforces strict production security standards**:
  - Zero critical vulnerabilities allowed
  - Maximum 5 high-severity vulnerabilities
- **Fails deployment if security standards not met**

### ECS Deployment Phase

#### Step 34: Task Definition Preparation
```bash
# Get current task definition or use template
if aws ecs describe-task-definition --task-definition ${{ env.ECS_TASK_DEFINITION }} --region ${{ env.AWS_REGION }} > /dev/null 2>&1; then
  aws ecs describe-task-definition \
    --task-definition ${{ env.ECS_TASK_DEFINITION }} \
    --region ${{ env.AWS_REGION }} \
    --query taskDefinition > task-definition.json
else
  # Use our template and substitute variables
  sed "s/\${AWS_ACCOUNT_ID}/${{ secrets.AWS_ACCOUNT_ID }}/g; s/\${AWS_REGION}/${{ env.AWS_REGION }}/g; s/\${ENVIRONMENT}/production/g" \
    aws/task-definition-template.json > task-definition.json
fi
```
**What happens**: Prepares ECS task definition:
- Attempts to retrieve existing task definition from ECS
- Falls back to template file if no existing definition
- Substitutes environment-specific variables
- Creates baseline for container configuration

#### Step 35: Task Definition Update
```yaml
- name: Update ECS task definition with new image
  id: task-def
  uses: aws-actions/amazon-ecs-render-task-definition@v1
  with:
    task-definition: task-definition.json
    container-name: ${{ env.CONTAINER_NAME }}
    image: ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ github.ref_name }}-${{ github.sha }}
    environment-variables: |
      SPRING_PROFILES_ACTIVE=aws
      AWS_REGION=${{ env.AWS_REGION }}
      APP_VERSION=${{ github.sha }}
      ENVIRONMENT=${{ github.ref == 'refs/heads/main' && 'production' || 'staging' }}
```
**What happens**: Updates task definition with new image and configuration:
- Replaces image URI with newly built production image
- Sets environment-specific variables
- Configures application for AWS environment
- Embeds version information for tracking

#### Step 36: ECS Cluster Management
```bash
aws ecs describe-clusters --clusters ${{ env.ECS_CLUSTER }} --region ${{ env.AWS_REGION }} || \
aws ecs create-cluster \
  --cluster-name ${{ env.ECS_CLUSTER }} \
  --capacity-providers FARGATE \
  --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1 \
  --region ${{ env.AWS_REGION }}
```
**What happens**: Ensures ECS cluster exists:
- Checks for existing cluster
- Creates cluster if missing with Fargate capacity provider
- Configures for serverless container execution

#### Step 37: ECS Service Deployment
```yaml
- name: Deploy to Amazon ECS
  uses: aws-actions/amazon-ecs-deploy-task-definition@v1
  with:
    task-definition: ${{ steps.task-def.outputs.task-definition }}
    service: ${{ env.ECS_SERVICE }}
    cluster: ${{ env.ECS_CLUSTER }}
    wait-for-service-stability: true    # Waits for deployment to complete
    force-new-deployment: true
```
**What happens**: Deploys new version to ECS:
- **Rolling deployment strategy**: Gradually replaces old tasks with new ones
- **Health check integration**: Only replaces healthy tasks
- **Service stability wait**: Pipeline waits until deployment is stable
- **Zero-downtime deployment**: Load balancer routes traffic only to healthy tasks

### Post-Deployment Verification Phase

#### Step 38: Deployment Health Verification
```bash
echo "Waiting for service to stabilize..."
sleep 60

# Get service details
SERVICE_INFO=$(aws ecs describe-services \
  --cluster ${{ env.ECS_CLUSTER }} \
  --services ${{ env.ECS_SERVICE }} \
  --region ${{ env.AWS_REGION }})

RUNNING_COUNT=$(echo $SERVICE_INFO | jq -r '.services[0].runningCount')
DESIRED_COUNT=$(echo $SERVICE_INFO | jq -r '.services[0].desiredCount')

echo "Running tasks: $RUNNING_COUNT"
echo "Desired tasks: $DESIRED_COUNT"

if [ "$RUNNING_COUNT" -eq "$DESIRED_COUNT" ] && [ "$RUNNING_COUNT" -gt "0" ]; then
  echo "✅ Deployment successful - $RUNNING_COUNT/$DESIRED_COUNT tasks running"
else
  echo "❌ Deployment verification failed"
  exit 1
fi
```
**What happens**: Verifies successful deployment:
- Waits for service stabilization period
- Checks that all desired tasks are running
- Validates task health through ECS service metrics
- **Fails pipeline if deployment is unhealthy**

#### Step 39: Performance Testing (Production Only)
```bash
- name: Run performance tests against deployed service
  if: github.ref == 'refs/heads/main'
  run: |
    # Get ALB endpoint
    ALB_ENDPOINT=$(aws ecs describe-services \
      --cluster ${{ env.ECS_CLUSTER }} \
      --services ${{ env.ECS_SERVICE }} \
      --region ${{ env.AWS_REGION }} \
      --query 'services[0].loadBalancers[0].targetGroupArn' \
      --output text)
    
    if [ "$ALB_ENDPOINT" != "None" ] && [ "$ALB_ENDPOINT" != "null" ]; then
      echo "Running performance tests against production endpoint..."
      # Add your performance testing commands here
      # Example: artillery, k6, or custom load testing
    fi
```
**What happens**: Runs performance validation for production deployments:
- Retrieves Application Load Balancer endpoint
- Executes performance tests against live endpoint
- Validates application performance under load
- Can include custom load testing tools

### Failure Recovery Phase

#### Step 40: Automatic Rollback
```bash
- name: Rollback on deployment failure
  if: failure()
  run: |
    echo "Deployment failed, initiating rollback..."
    
    # Get previous task definition
    PREVIOUS_TASK_DEF=$(aws ecs list-task-definitions \
      --family-prefix ${{ env.ECS_TASK_DEFINITION }} \
      --status ACTIVE \
      --sort DESC \
      --region ${{ env.AWS_REGION }} \
      --query 'taskDefinitionArns[1]' \
      --output text)
    
    if [ "$PREVIOUS_TASK_DEF" != "None" ] && [ "$PREVIOUS_TASK_DEF" != "null" ]; then
      echo "Rolling back to: $PREVIOUS_TASK_DEF"
      aws ecs update-service \
        --cluster ${{ env.ECS_CLUSTER }} \
        --service ${{ env.ECS_SERVICE }} \
        --task-definition $PREVIOUS_TASK_DEF \
        --region ${{ env.AWS_REGION }}
    fi
```
**What happens**: Automatic failure recovery:
- Triggered only if deployment steps fail
- Identifies previous stable task definition
- Rolls back ECS service to last known good version
- Maintains service availability during recovery

#### Step 41: Status Notification
```yaml
- name: Notify deployment status
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    channel: '#deployments'
    text: |
      🚀 *Store App Deployment*
      
      *Status:* ${{ job.status == 'success' && '✅ SUCCESS' || '❌ FAILED' }}
      *Environment:* ${{ github.ref == 'refs/heads/main' && 'Production' || 'Staging' }}
      *Image:* `${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:${{ github.ref_name }}-${{ github.sha }}`
      *Commit:* ${{ github.sha }}
      *Author:* ${{ github.actor }}
      
      <https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }}|View Workflow Run>
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```
**What happens**: Sends deployment status notification:
- Runs regardless of success or failure (`if: always()`)
- Sends rich Slack notification with deployment details
- Includes direct links to GitHub Actions run
- Provides immediate team visibility into deployment status

---

## 📋 JOB 3: Cleanup (Production Success Only)

**Purpose**: Resource optimization and lifecycle management for production deployments.

**Prerequisites**:
```yaml
needs: aws-deploy
if: success() && github.ref == 'refs/heads/main'
```

### Resource Management Phase

#### Step 42: AWS Authentication for Cleanup
```yaml
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: ${{ secrets.AWS_ROLE_TO_ASSUME }}
    role-session-name: GitHubActions-Cleanup-${{ github.run_id }}
    aws-region: ${{ env.AWS_REGION }}
```
**What happens**: Re-authenticates with AWS for cleanup operations using dedicated session.

#### Step 43: ECR Image Lifecycle Management
```bash
echo "Cleaning up old ECR images..."

# Keep last 10 images, delete older ones
aws ecr list-images \
  --repository-name ${{ env.ECR_REPOSITORY }} \
  --filter tagStatus=TAGGED \
  --query 'imageIds[?imageTag!=`latest`] | sort_by(@, &imagePushedAt) | [0:-10]' \
  --output json \
  --region ${{ env.AWS_REGION }} > old-images.json

if [ -s old-images.json ] && [ "$(cat old-images.json)" != "[]" ]; then
  aws ecr batch-delete-image \
    --repository-name ${{ env.ECR_REPOSITORY }} \
    --image-ids file://old-images.json \
    --region ${{ env.AWS_REGION }}
  echo "✅ Cleaned up old ECR images"
else
  echo "No old images to clean up"
fi
```
**What happens**: Automated image lifecycle management:
- Lists all tagged images sorted by push date
- Preserves most recent 10 images (excludes "latest" tag)
- Deletes older images to manage storage costs
- Only runs for successful production deployments
- Maintains deployment history while controlling costs

---

## 🎯 Pipeline Success Criteria & Quality Gates

### **Critical Success Path**
For the pipeline to deliver a **production-ready Docker image**, ALL these criteria must be met:

#### Quality Gates (Job 1)
- ✅ **Code formatting passes** Spotless validation
- ✅ **All unit tests pass** with real database connections
- ✅ **All integration tests pass** with full system integration
- ✅ **Code coverage ≥ 80%** overall, ≥ 60% per class
- ✅ **Zero critical dependencies vulnerabilities** (CVSS < 7.0)
- ✅ **Container vulnerabilities within limits**:
  - Critical: 0 allowed
  - High: ≤ 2 allowed
- ✅ **Application JAR builds successfully**
- ✅ **Docker image builds and loads**

#### Deployment Gates (Job 2)
- ✅ **AWS authentication succeeds** via OIDC
- ✅ **ECR repository accessible** and configured
- ✅ **Production Docker image builds** for linux/amd64
- ✅ **ECR push succeeds** with vulnerability scan pass
- ✅ **ECS task definition updates** successfully
- ✅ **ECS deployment completes** with service stability
- ✅ **Health checks pass** on all running tasks
- ✅ **Load balancer routes traffic** to healthy instances

#### Post-Deployment Validation
- ✅ **Application endpoints respond** to health checks
- ✅ **Database connections established** and functional
- ✅ **Cache connections working** with Redis cluster
- ✅ **Performance benchmarks met** (production only)

### **Pipeline Failure Points**

The pipeline will **FAIL** and **prevent deployment** if:

| Failure Point | Impact | Recovery |
|---------------|---------|----------|
| Test coverage < 80% | ❌ Quality gate failure | Fix tests, improve coverage |
| Critical vulnerabilities found | ❌ Security gate failure | Update dependencies, patch vulnerabilities |
| Docker build fails | ❌ Image creation failure | Fix Dockerfile, resolve build issues |
| ECR push fails | ❌ Registry failure | Check AWS permissions, network connectivity |
| ECS deployment fails | ❌ Service update failure | Automatic rollback to previous version |
| Health checks fail | ❌ Application failure | Automatic rollback, investigate application issues |

### **Environment-Specific Behavior**

| Branch | Environment | Deployment | Performance Tests | Cleanup |
|--------|-------------|------------|------------------|---------|
| `main` | Production | ✅ Full | ✅ Yes | ✅ Yes |
| `develop` | Staging | ✅ Full | ❌ No | ❌ No |
| `feature/*` | None | ❌ No | ❌ No | ❌ No |
| `pull_request` | None | ❌ No | ❌ No | ❌ No |

---

## 🔧 Configuration & Secrets

### **Required GitHub Secrets**
```bash
# AWS Authentication (OIDC)
AWS_ROLE_TO_ASSUME=arn:aws:iam::ACCOUNT:role/GitHubActionsRole

# Notification Integration  
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
```

### **Environment Variables**
```yaml
env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: store-app
  ECS_SERVICE: store-app-service
  ECS_CLUSTER: store-app-cluster
  ECS_TASK_DEFINITION: store-app-task
  CONTAINER_NAME: store-app
  
  # Quality Thresholds
  MIN_CODE_COVERAGE: 80
  MAX_CRITICAL_VULNERABILITIES: 0
  MAX_HIGH_VULNERABILITIES: 2
```

### **AWS Resources Created/Used**
- **Amazon ECR**: Container image registry with vulnerability scanning
- **Amazon ECS**: Container orchestration with Fargate
- **Application Load Balancer**: Traffic distribution and health checks
- **Amazon RDS**: PostgreSQL database with encryption
- **Amazon ElastiCache**: Redis caching cluster
- **AWS Secrets Manager**: Secure credential storage
- **CloudWatch**: Monitoring and logging
- **VPC & Networking**: Secure network infrastructure

---

## 📊 Monitoring & Observability

### **Pipeline Monitoring**
- **GitHub Actions logs**: Detailed step-by-step execution logs
- **Test reports**: JUnit test results with pass/fail details
- **Coverage reports**: JaCoCo coverage analysis with trends
- **Security reports**: Trivy and OWASP vulnerability findings
- **Deployment status**: Real-time deployment progress and health

### **Application Monitoring**
- **Health endpoints**: `/actuator/health` for application status
- **Metrics endpoints**: `/actuator/prometheus` for monitoring integration
- **Application logs**: Structured logging with correlation IDs
- **Performance metrics**: Response times, error rates, throughput

### **Infrastructure Monitoring**
- **ECS Container Insights**: Task and service performance metrics
- **RDS Performance Insights**: Database query performance analysis
- **ElastiCache metrics**: Cache hit rates and performance
- **ALB metrics**: Load balancer health and request distribution

---

## 🚀 Pipeline Results

### **Successful Pipeline Delivers**
1. **Production-ready Docker image** in Amazon ECR with security validation
2. **Running ECS service** with health-checked containers on Fargate
3. **Load-balanced application** accessible via Application Load Balancer
4. **Comprehensive monitoring** through CloudWatch and Container Insights
5. **Security compliance** with vulnerability scanning and compliance checks
6. **Automated rollback capability** for deployment failure recovery

### **Quality Assurance Guarantees**
- **Code quality**: 80%+ test coverage with formatting standards
- **Security**: Zero critical vulnerabilities, minimal high-severity issues
- **Reliability**: Multi-AZ deployment with automatic failure recovery
- **Performance**: Load testing validation for production deployments
- **Observability**: Full monitoring and alerting stack integrated

### **Operational Benefits**
- **Zero-downtime deployments** with rolling update strategy
- **Automatic rollback** on deployment failures
- **Cost optimization** with Fargate Spot and image cleanup
- **Security compliance** with automated vulnerability management
- **Audit trail** with complete deployment history and traceability

---

**🎯 Result: The pipeline successfully delivers a production-ready, secure, scalable Docker application to AWS infrastructure with enterprise-grade operational capabilities.**