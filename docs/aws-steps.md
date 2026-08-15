# AWS Steps

Quick-reference checklist. For full click-by-click instructions, commands, and screenshots, see [AWS_DEPLOYMENT_GUIDE.md](../AWS_DEPLOYMENT_GUIDE.md).

1. **Create IAM User** — `employee-service-deployer`, programmatic access only, `AmazonS3FullAccess` + `AmazonEC2FullAccess` ✅
2. **Create Key Pair** — `springboot-key.pem`, RSA, reused across instances ✅
3. **Launch EC2** — Ubuntu 24.04, `t3.micro`, `employee-service-server` ✅
4. **Install Java (and Maven)** — OpenJDK 21 + Maven, needed to build the JAR on-instance ✅
5. **Create RDS** (chosen over local MySQL) — MySQL 8.4, `db.t4g.micro`, not publicly accessible, isolated schema + user ✅
6. **Deploy Spring Boot JAR** — clone from GitHub, build with Maven, run with `nohup` so it survives SSH disconnects ✅
7. **Configure Security Group** — SSH from My IP, app port from My IP (temporary) and later from the ALB's SG ✅
8. **Test Application** — direct `curl` to the instance + full Postman CRUD/auth flow ✅
9. **Create S3 Bucket** — `neelu-employee-profile-images-2026`, all public access blocked ✅
10. **Create IAM Role for EC2** — `employee-service-ec2-role`, attached to the instance, no static access keys ✅
11. **Upload Files to S3** — profile-picture upload/download/delete wired into `EmployeeController`, verified end-to-end via Postman ✅
12. **Create ALB** — Target Group with `/health` check, ALB across 2 AZs, dedicated `employee-service-alb-sg` ✅
13. **Create Auto Scaling Group** — not yet done ⏳
14. **Configure CloudWatch Alarm** — not yet done ⏳
