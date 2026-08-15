# Employee Management System — AWS Learning Project

A hands-on AWS learning project: deploying a Spring Boot employee management API through IAM, EC2, RDS, S3, and an Application Load Balancer, with Auto Scaling and CloudWatch still to come.

## Where to look

- **[AWS_DEPLOYMENT_GUIDE.md](AWS_DEPLOYMENT_GUIDE.md)** — the full build log: every AWS step in order, with commands, screenshots, and the reasoning behind each decision. Start here.
- **[docs/architecture.md](docs/architecture.md)** — standalone architecture diagram and traffic flow.
- **[docs/aws-steps.md](docs/aws-steps.md)** — quick-reference checklist of the 14 AWS steps.
- **[docs/project-structure.md](docs/project-structure.md)** — planned service module breakdown.
- **[employee-service/](employee-service)** — the Spring Boot application (Employee CRUD, JWT auth, S3 profile pictures).

## Status

| Phase | What | Status |
|---|---|---|
| 1 | Spring Boot + MySQL, Employee CRUD | ✅ |
| 2 | Spring Security + JWT | ✅ |
| 3 | Deploy to EC2 | ✅ |
| 4 | Application Load Balancer | ✅ |
| 5 | RDS | ✅ |
| 6 | S3 File Upload | ✅ |
| 7 | SQS Notification Service | ⏳ not started |
| 8 | Redis Cache | ⏳ not started |
| 9 | Auto Scaling + CloudWatch | ⏳ not started |
