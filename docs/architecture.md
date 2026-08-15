# Architecture

Current state and target state for `employee-service` on AWS. For the step-by-step build log (including screenshots), see [AWS_DEPLOYMENT_GUIDE.md](../AWS_DEPLOYMENT_GUIDE.md#architecture) — this file is the standalone reference diagram.

```mermaid
flowchart TB
    Client(["Client\nPostman / Browser"])
    Internet(("Internet"))

    subgraph AWS["AWS Account — eu-north-1"]
        IGW["Internet Gateway"]

        subgraph VPC["VPC: vpc-0023eaab02a24c1cc (172.31.0.0/16)"]
            ALB["Application Load Balancer\nemployee-service-alb\nSG: employee-service-alb-sg\nHTTP :80"]
            TG["Target Group: employee-service-tg\nHTTP :8081, health check /health"]

            subgraph AZ1["AZ: eu-north-1a"]
                EC2["EC2: employee-service-server\nUbuntu 24.04, t3.micro\nSG: launch-wizard-4\nSpring Boot :8081\nIAM Role: employee-service-ec2-role"]
            end

            subgraph AZ2["AZ: eu-north-1c"]
                RDS[("RDS MySQL: database-1\nSG: launch-wizard-2\nemployee_db schema, employee_svc user")]
                ASG["Auto Scaling Group\n(planned — Step 13)"]
            end
        end

        S3[("S3: neelu-employee-profile-images-2026\n(outside VPC — IAM-governed, not SG-governed)")]
        CW["CloudWatch Alarm\n(planned — Step 14)"]
    end

    Client --> Internet --> IGW --> ALB
    ALB --> TG --> EC2
    EC2 -->|JDBC :3306| RDS
    EC2 -->|IAM role, no static keys| S3
    ALB -.->|planned| ASG
    ASG -.-> EC2
    CW -.->|monitors CPU| ASG
```

## Traffic flow

`Client → ALB (:80) → Target Group → EC2 (:8081, Spring Boot) → RDS (:3306) / S3`

The ALB is the only public entry point on the app path — the EC2 instance's security group only accepts port 8081 from the ALB's security group (plus a temporary rule from the developer's own IP, used before the ALB existed). RDS is not publicly accessible at all; it only accepts connections from the EC2 instance's security group.

## Identity and access

- **`employee-service-deployer`** (IAM user) — used from the local machine to launch/configure AWS resources via the console; not used by the running application.
- **`employee_svc`** (RDS user) — scoped only to the `employee_db` schema on `database-1`, which also hosts an unrelated project's database on the same RDS instance. No shared credentials between the two.
- **`employee-service-ec2-role`** (IAM role, attached to the EC2 instance) — grants S3 access via the AWS SDK's default credential chain. No static AWS access keys live on the instance or in the repo.

## Why S3 sits outside the VPC boundary in the diagram

Unlike EC2/RDS/ALB, S3 isn't a VPC-attached resource — access to it is controlled entirely by IAM (the instance role) and, optionally, bucket policies, not security groups. That's why `employee-service-ec2-role` exists: it's the S3 equivalent of what a security group does for network-level resources.

## Planned (not yet built)

- **Auto Scaling Group** (Step 13) — replace the single manually-managed EC2 instance with a Launch Template + ASG behind the existing target group.
- **CloudWatch Alarm** (Step 14) — CPU-utilization-based scaling policy once the ASG exists.
- **SQS Notification Service** (README Phase 7) and **Redis Cache** (README Phase 8) — not started.
