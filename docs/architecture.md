# Architecture

Browser
   |
Route53
   |
Application Load Balancer
   |
+----------------------+
| EC2-1 | EC2-2        |
+----------------------+
   |       | 
   +-------+
       |
   RDS
   S3
   Redis
   SQS
