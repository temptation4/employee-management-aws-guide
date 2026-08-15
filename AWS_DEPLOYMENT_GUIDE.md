# Employee Management System — AWS Deployment Guide

A step-by-step record of deploying `employee-service` (Spring Boot + MySQL + JWT) to AWS: IAM, EC2, RDS, and an Application Load Balancer, with the remaining steps still to come.

This follows the phases in the [root README](README.md) and maps directly onto the 14-step checklist in [03-aws-steps/aws-steps.md](03-aws-steps/aws-steps.md).

---

## Architecture

```mermaid
flowchart LR
    Client["Client\n(Postman / Browser)"]
    ALB["Application Load Balancer\nemployee-service-alb\nPort 80"]
    TG["Target Group\nemployee-service-tg\nHTTP:8081, /health"]
    EC2["EC2 Instance\nemployee-service-server\nUbuntu 24.04, t3.micro\nSpring Boot :8081"]
    RDS[("RDS MySQL\ndatabase-1\nemployee_db schema")]

    Client -->|HTTP :80| ALB
    ALB --> TG
    TG --> EC2
    EC2 -->|JDBC :3306\nemployee_svc user| RDS

    S3[("S3 Bucket\n(Step 9-11 — planned)")]
    ASG["Auto Scaling Group\n(Step 13 — planned)"]
    CW["CloudWatch Alarm\n(Step 14 — planned)"]
    EC2 -.->|not yet wired| S3
    ALB -.->|planned| ASG
    ASG -.-> EC2
    CW -.->|monitors| ASG
```

**Security groups involved:**

| Security Group | Attached To | Key Inbound Rules |
|---|---|---|
| `employee-service-alb-sg` | ALB | HTTP 80 from `0.0.0.0/0` |
| `launch-wizard-4` | EC2 instance | SSH 22 from My IP, TCP 8081 from `employee-service-alb-sg` |
| `launch-wizard-2` | RDS instance | MySQL 3306 from `launch-wizard-4` (and one other project's EC2 SG) |

---

## Application: Employee CRUD + JWT Auth

Before touching AWS, the app itself was built at [`02-springboot-project/employee-service`](02-springboot-project/employee-service):

- **Entities**: `Employee`, `User`, `Role` (enum)
- **Layers**: Controller → Service (interface) → ServiceImpl → Repository, with request/response DTOs
- **Security**: `JwtUtil`, `JwtFilter`, `SecurityConfig` — stateless JWT auth, BCrypt password hashing
- **Endpoints**: `/api/auth/register`, `/api/auth/login`, `/api/employees/**` (CRUD, JWT-protected), `/api/users/me`, `/api/users` (ADMIN only), `/health` (public, for the ALB)

Tested locally end-to-end with Postman — creating an employee via `POST /api/employees` and reading it back via `GET /api/employees` both returning `200 OK` with the expected JSON.

<img width="1756" height="1354" alt="image" src="https://github.com/user-attachments/assets/aba68d9e-8407-4376-bf72-82ee28bf18b8" />


---

## The 14 AWS Steps

### ✅ Step 1 — Create IAM User

1. Sign in to the [AWS Console](https://console.aws.amazon.com/) as your root or admin account.
2. Go to **IAM** → **Users** (left sidebar) → **Create user**.
3. **User name**: `employee-service-deployer` — this represents the app/deployment, not a personal login.
4. Leave **"Provide user access to the AWS Management Console"** unchecked — this user only needs programmatic access, not console login.
5. On the **Permissions** step, choose **Attach policies directly** and attach:
   - `AmazonS3FullAccess`
   - `AmazonEC2FullAccess`
   
   (`FullAccess` is acceptable for a learning project; a production setup would scope these down to least-privilege.)
6. Skip tags → **Create user**.
7. Open the new user → **Security credentials** tab → **Access keys** → **Create access key**.
   - Use case: **Command Line Interface (CLI)**
   - Confirm the checkbox → **Create access key**
8. Copy the **Access Key ID** and **Secret Access Key** immediately — AWS only shows the secret once. Store them in `~/.aws/credentials` (never commit these to a repo):

```ini
[default]
aws_access_key_id = YOUR_ACCESS_KEY_ID
aws_secret_access_key = YOUR_SECRET_ACCESS_KEY
```

<img width="2932" height="1494" alt="image" src="https://github.com/user-attachments/assets/5c86f6c9-32bd-4ce2-a329-aede49ecc9f2" />


### ✅ Step 2 — Create Key Pair

1. **EC2** → left sidebar under **Network & Security** → **Key Pairs** → **Create key pair**.
2. **Name**: `springboot-key`.
3. **Key pair type**: `RSA`.
4. **Private key file format**: `.pem` (for OpenSSH on macOS/Linux — use `.ppk` only for PuTTY on Windows).
5. **Create key pair** — the `.pem` file downloads automatically. This is the only time AWS gives you the private key; there's no way to re-download it.
6. Lock down the file permissions, or SSH will refuse to use it:

```bash
chmod 400 ~/Downloads/springboot-key.pem
mkdir -p ~/.ssh/aws-keys && mv ~/Downloads/springboot-key.pem ~/.ssh/aws-keys/
```

A key pair isn't tied to one instance or one project — this same `springboot-key.pem` was reused when launching the second EC2 instance for this project, rather than creating a new one. (Key pairs are region-scoped, though: one created in `eu-north-1` won't appear when launching an instance in a different region.)

### ✅ Step 3 — Launch EC2

1. **EC2** → **Instances** → **Launch instances**.
2. **Name**: `employee-service-server`.
3. **AMI**: **Ubuntu 24.04 LTS** (Canonical).
4. **Instance type**: `t3.micro` (free-tier eligible).
5. **Key pair**: select `springboot-key` from the dropdown.
6. **Network settings** → **Edit** → configure the security group:
   - **SSH (22)**: source `My IP`.
   - **Custom TCP (8081)**: source `My IP` (temporary, for testing before the ALB existed).
7. **Configure storage**: default 8GB gp3.
8. **Launch instance**.
9. Wait for **Instance state** = `Running` and **Status check** = `2/2 checks passed`.
10. Note the **Public IPv4 address** from the instance details page.
11. SSH in:

<img width="2914" height="1324" alt="image" src="https://github.com/user-attachments/assets/4cd17f5b-d24e-4dd1-82cf-1a5835a536ed" />

```bash
ssh -i ~/.ssh/aws-keys/springboot-key.pem ubuntu@<PUBLIC_IP>
```

Launched as a *second*, dedicated instance rather than reusing an EC2 already running an unrelated project — keeps the two isolated (no port collisions, a crash in one doesn't affect the other).


<img width="1848" height="890" alt="image" src="https://github.com/user-attachments/assets/e4a71f9a-a016-4b3b-a36e-c8cbbef362c2" />



### ✅ Step 4 — Install Java

On the EC2 instance, over SSH:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-21-jdk
java -version
```

Maven was also needed to build the JAR on the instance (not in the original checklist, but required for Step 6):

```bash
sudo apt install -y maven
mvn -version
```

<img width="1816" height="494" alt="image" src="https://github.com/user-attachments/assets/2f69f9bf-dc18-4669-8e4c-7d494634c498" />
`

### ✅ Step 5 — Create RDS (instead of local MySQL)

Chose RDS over installing MySQL locally on the instance, since RDS is the actual AWS skill this checklist is building toward.

**Create the instance:**

1. **RDS** → **Databases** → **Create database**.
2. **Creation method**: `Standard create`.
3. **Engine type**: `MySQL`.
4. **Templates**: `Free tier`.
5. **Settings**: DB instance identifier `database-1`, master username `admin`, set a real master password.
6. **Instance configuration**: `db.t4g.micro` (free-tier).
7. **Storage**: default 20GB gp2.
8. **Connectivity**:
   - **VPC**: same VPC as the EC2 instance (`vpc-0023eaab02a24c1cc`) — required, since security-group-to-security-group references don't work across different VPCs without peering.
   - **Public access**: `No` — reachable only from resources inside the VPC via security group, never the open internet.
   - **VPC security group**: create new, `launch-wizard-2` (auto-named).
9. **Create database** — takes a few minutes to reach `Available`.

<img width="2940" height="948" alt="image" src="https://github.com/user-attachments/assets/9100ea81-8ddb-421c-86b2-df28660b9d81" />


**Networking:**

1. Open the RDS security group (`launch-wizard-2`) → **Inbound rules** → **Edit inbound rules** → **Add rule**.
2. Type: `MYSQL/Aurora` (auto-fills port 3306).
3. Source: select **the EC2 instance's security group** (`launch-wizard-4`) from the dropdown — not a raw IP. This means "only things using that security group can reach me."
4. **Save rules**.

<img width="2352" height="600" alt="image" src="https://github.com/user-attachments/assets/472fca5c-14af-400a-80e0-3e24bf8b67ba" />


**Isolated schema + user:** this RDS instance also hosts an unrelated project's database, so rather than reusing the master (`admin`) credentials, created a dedicated schema and user scoped only to this project.

On the EC2 instance:

```bash
sudo apt install -y mysql-client
curl -o global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
mysql -h <rds-endpoint> -P 3306 -u admin -p --ssl-mode=VERIFY_IDENTITY --ssl-ca=./global-bundle.pem
```

At the `mysql>` prompt:

```sql
CREATE DATABASE employee_db;
CREATE USER 'employee_svc'@'%' IDENTIFIED BY '********';
GRANT ALL PRIVILEGES ON employee_db.* TO 'employee_svc'@'%';
FLUSH PRIVILEGES;
```

### ✅ Step 6 — Deploy Spring Boot JAR

1. Pushed the project to GitHub: [github.com/temptation4/employee-management-aws-guide](https://github.com/temptation4/employee-management-aws-guide) (public repo — real credentials kept out of the committed `application.properties` via environment-variable placeholders, e.g. `${DB_URL:jdbc:mysql://localhost:3306/employee_db}`).
2. On the EC2 instance:

```bash
git clone https://github.com/temptation4/employee-management-aws-guide.git
cd employee-management-aws-guide/02-springboot-project/employee-service

export DB_URL="jdbc:mysql://<rds-endpoint>:3306/employee_db"
export DB_USERNAME="employee_svc"
export DB_PASSWORD="********"
export JWT_SECRET="********"

mvn clean package -DskipTests
nohup java -jar target/employee-service-1.0.0.jar > employee-service.log 2>&1 &
```

(Adding those `export` lines to `~/.bashrc` keeps them set across future SSH sessions, rather than re-exporting every time.)

<img width="2882" height="988" alt="image" src="https://github.com/user-attachments/assets/9c73a488-aee5-4d8f-86c5-9d815e2e1fdd" />


### ✅ Step 7 — Configure Security Group

`launch-wizard-4` (EC2 instance SG) ended up with:

| Type | Port | Source | Purpose |
|---|---|---|---|
| SSH | 22 | My IP | Remote shell access to manage the server |
| Custom TCP | 8081 | My IP | Direct testing before the ALB existed |
| Custom TCP | 8081 | `employee-service-alb-sg` | Lets the ALB's health checks and forwarded traffic through (added in Step 12) |

To add a rule: open the SG → **Edit inbound rules** → **Add rule** (don't remove existing ones) → set Type/Port/Source → **Save rules**.

<img width="2940" height="1232" alt="image" src="https://github.com/user-attachments/assets/dc2c175a-d55e-4d22-bbdd-90c257070285" />


### ✅ Step 8 — Test Application

Directly against the instance (before the ALB was in place):

```bash
curl http://<EC2_PUBLIC_IP>:8081/health
# → OK
```

Also exercised the full CRUD + auth flow via Postman: register → login (JWT returned) → create/read/update/delete an employee, all against the live EC2-hosted app.

### ⏳ Step 9 — Create S3 Bucket

*Not yet wired into the app — a bucket already exists from earlier, planned for profile-picture upload.*

How this will be done, following the same pattern as [`springboot-s3-demo`](../springboot-s3-demo):

1. **S3** → **Create bucket**.
2. Bucket name must be globally unique, e.g. `employee-service-profile-images-<suffix>`.
3. Region: same as the rest of the infrastructure (`eu-north-1`).
4. Block Public Access: leave **all four boxes checked** (block all public access) — access will go through the app using pre-signed URLs, not public bucket policies.
5. **Create bucket**.

### ⏳ Step 10 — Create IAM Role for EC2

Planned approach (an IAM Role, not static access keys, since roles auto-rotate credentials and never live in a file on disk):

1. **IAM** → **Roles** → **Create role**.
2. Trusted entity type: `AWS service` → Use case: `EC2`.
3. Attach policy: `AmazonS3FullAccess` (or a scoped-down custom policy limited to the specific bucket).
4. Name it, e.g. `employee-service-ec2-role`.
5. Attach it to the running instance: **EC2** → select `employee-service-server` → **Actions** → **Security** → **Modify IAM role** → select `employee-service-ec2-role`.
   <img width="2940" height="1130" alt="image" src="https://github.com/user-attachments/assets/b4c40df2-6af8-4b9c-9896-c8cf83f152e3" />


### ⏳ Step 11 — Upload Files to S3

*Depends on Steps 9 and 10.* Will add an `S3Service` (mirroring the one in `springboot-s3-demo`) and wire it into `EmployeeService` for profile-picture upload/download/delete via pre-signed URLs.

### ✅ Step 12 — Create ALB

Built out of order relative to the checklist (before S3), since it was the next unfinished phase in the README's roadmap.

**Target Group:**

1. **EC2** → **Target Groups** → **Create target group**.
2. Target type: `Instances`.
3. Name: `employee-service-tg`.
4. Protocol/Port: `HTTP` / `8081`.
5. VPC: `vpc-0023eaab02a24c1cc`.
6. Health check path: `/health` — a dedicated public endpoint (`HealthController` + `SecurityConfig` permitAll) added specifically for this, since every other endpoint requires a JWT and would always fail health checks.
7. Register `employee-service-server` as the target on port 8081 → **Create target group**.

> 📸 **Screenshot needed:** `docs/screenshots/08-target-group-healthy.png` — the Target Group detail page showing **1 Healthy** target.

**Load Balancer:**

1. **EC2** → **Load Balancers** → **Create load balancer** → **Application Load Balancer**.
2. Name: `employee-service-alb`. Scheme: `Internet-facing`.
3. VPC: `vpc-0023eaab02a24c1cc`. Select **2 Availability Zones** (`eu-north-1a`, `eu-north-1c`) — ALBs require at least 2 AZs even though the EC2 instance only lives in one.
4. Security group: created a dedicated `employee-service-alb-sg` (HTTP 80 from `0.0.0.0/0` inbound, all traffic outbound) rather than reusing the `default` SG.
5. Listener: `HTTP` : `80` → forward to `employee-service-tg`.
6. **Create load balancer**.

> 📸 **Screenshot needed:** `docs/screenshots/09-alb-active.png` — the Load Balancers list showing `employee-service-alb` with State = **Active**.

**Closing the loop:** added the inbound rule on `launch-wizard-4` (Step 7's table, row 3) allowing 8081 from `employee-service-alb-sg` — without it, the ALB's health checks and forwarded traffic are blocked even with the app running correctly.

**Verification:**

```bash
curl http://<ALB_DNS_NAME>/health
# → OK
```

Traffic now flows: **Client → ALB (port 80) → Target Group → EC2 (port 8081) → Spring Boot → RDS**.

> 📸 **Screenshot needed:** `docs/screenshots/10-curl-via-alb.png` — terminal showing the `curl` call against the ALB DNS name returning `OK`.

### ⏳ Step 13 — Create Auto Scaling Group

*Not yet done.* Planned approach:

1. Create a **Launch Template** from the current `employee-service-server` configuration (AMI, instance type, key pair, security group, and a user-data script that pulls the latest code and starts the JAR on boot).
2. **EC2** → **Auto Scaling Groups** → **Create Auto Scaling group**, using that launch template.
3. Attach it to the existing `employee-service-tg` target group, so the ALB automatically picks up new instances.
4. Set desired/min/max capacity (e.g. min 1, desired 1, max 3 for a learning project).

### ⏳ Step 14 — Configure CloudWatch Alarm

*Not yet done.* Planned once the ASG exists:

1. **CloudWatch** → **Alarms** → **Create alarm**.
2. Metric: `CPUUtilization` on the Auto Scaling Group.
3. Threshold: e.g. scale out above 70% for 5 minutes.
4. Action: trigger the ASG's scaling policy.

---

## What's Next

In order:

1. **Step 9-11** — S3 bucket wiring: IAM role for EC2, profile-picture upload/download endpoints
2. **Step 13** — Auto Scaling Group
3. **Step 14** — CloudWatch alarm
4. Remaining README phases not yet started: **Phase 7 (SQS)**, **Phase 8 (Redis)**

---

## Screenshot Checklist

Save each into `docs/screenshots/` with the exact filename below, then let me know and I'll wire them into this doc:

| Filename | What it should show |
|---|---|
| `00-postman-collection.png` | Postman: employee CRUD request/response |
| `01-iam-user.png` | IAM user summary/permissions page |
| `03-ec2-launch-review.png` | EC2 "Launch instance" review pane |
| `04-java-maven-version.png` | Terminal: `java -version` + `mvn -version` |
| `05-rds-summary.png` | RDS instance Summary panel |
| `06-rds-security-group-rules.png` | RDS SG inbound rules (MySQL/Aurora sourced from EC2 SG) |
| `07-app-startup-logs.png` | Terminal: Spring Boot startup logs, Tomcat on 8081 |
| `07b-ec2-security-group-rules.png` | EC2 instance SG inbound rules (SSH, 8081 ×2) |
| `08-target-group-healthy.png` | Target Group page showing 1 Healthy |
| `09-alb-active.png` | Load Balancers list, `employee-service-alb` = Active |
| `10-curl-via-alb.png` | Terminal: `curl` through the ALB DNS returning `OK` |
