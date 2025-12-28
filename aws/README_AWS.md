# AWS deployability (ECS Fargate + optional RDS/Redis)

This repo includes a CloudFormation template:
- aws/cloudformation/ecs-fargate-safe.yml

## Safety first (default = NO AWS resources)
The template has a parameter:
- EnableDeploy (default: "false")

If you create a stack with defaults, it will create **nothing**.
You must explicitly set EnableDeploy="true" to create resources.

## What it deploys (when EnableDeploy=true)
- VPC (2 public + 2 private subnets)
- ALB (HTTP 80)
- ECS Fargate cluster
- Frontend service (nginx static)
- Backend service (Spring Boot)
- ALB path routing:
    - /api/* -> backend
    - /ws-chat* -> backend
    - everything else -> frontend
- Optional (paid):
    - RDS Postgres (db.t4g.micro)
    - ElastiCache Redis (cache.t4g.micro)

## Kafka / MSK
The template does NOT create MSK.
Instead, set parameter KafkaBootstrapServers to your Kafka/MSK bootstrap string.
For MSK Serverless, AWS shows you copy the Endpoint under "View client information".

## Build images (example)
Backend:
docker build -t chat-backend:latest ./backend

Frontend (production dockerfile):
docker build -t chat-frontend:latest -f ./frontend/Dockerfile.prod ./frontend

Push to ECR (or any registry) and set BackendImage / FrontendImage accordingly.

## Costs
If you enable RDS/ElastiCache, you will be billed while they exist.
To keep costs at $0, do NOT set EnableDeploy=true.
