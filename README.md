# OrthoCareChain — Complete Backend Setup Guide

## Project Structure (57 Files)

```
orthocarechain/
├── pom.xml
└── src/main/
    ├── java/com/orthocarechain/
    │   ├── OrthoCareChainApplication.java       ← Main entry point
    │   ├── config/
    │   │   ├── AsyncConfig.java                 ← Thread pool for async emails
    │   │   ├── CloudinaryConfig.java            ← Cloudinary bean setup
    │   │   ├── DataInitializer.java             ← Seeds default admin on startup
    │   │   └── SecurityConfig.java              ← JWT + Spring Security + CORS
    │   ├── controller/
    │   │   ├── AdminController.java             ← ADMIN-only user management
    │   │   ├── AuthController.java              ← Login, Register, Refresh, Logout
    │   │   ├── DoctorController.java            ← Doctor profile & listing
    │   │   ├── MedicalImageController.java      ← Cloudinary upload/delete
    │   │   ├── PatientController.java           ← Patient profile & listing
    │   │   ├── PrescriptionController.java      ← Rx management + Pharmacy access
    │   │   └── ReportController.java            ← Core visit reports + PDF download
    │   ├── dto/
    │   │   ├── request/  (LoginRequest, RegisterRequest, ReportRequest,
    │   │   │              PrescriptionRequest, DoctorProfileRequest,
    │   │   │              PatientProfileRequest)
    │   │   └── response/ (ApiResponse<T>, JwtResponse, ReportResponse,
    │   │                  PrescriptionResponse, MedicalImageResponse,
    │   │                  DoctorResponse, PatientResponse, UserResponse)
    │   ├── entity/
    │   │   ├── User.java                        ← Auth entity (all roles)
    │   │   ├── Doctor.java                      ← Doctor profile (1:1 User)
    │   │   ├── Patient.java                     ← Patient profile (1:1 User)
    │   │   ├── Report.java                      ← Visit report (core entity)
    │   │   ├── Prescription.java                ← Medicine per report
    │   │   └── MedicalImage.java                ← Cloudinary image reference
    │   ├── enums/
    │   │   ├── Role.java                        ← ADMIN, DOCTOR, PATIENT, PHARMACY
    │   │   ├── SeverityLevel.java               ← MILD, MODERATE, SEVERE, CRITICAL
    │   │   └── ImageType.java                   ← XRAY, MRI, CT_SCAN, ULTRASOUND
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java      ← Centralized error responses
    │   │   ├── ResourceNotFoundException.java
    │   │   ├── DuplicateResourceException.java
    │   │   └── UnauthorizedAccessException.java
    │   ├── repository/   (UserRepository, DoctorRepository, PatientRepository,
    │   │                  ReportRepository, PrescriptionRepository,
    │   │                  MedicalImageRepository)
    │   ├── scheduler/
    │   │   └── ReminderScheduler.java           ← Daily drug & visit email reminders
    │   ├── security/
    │   │   ├── JwtUtils.java                    ← Token generate / validate
    │   │   ├── JwtAuthenticationFilter.java     ← Reads Bearer token per request
    │   │   ├── JwtAuthEntryPoint.java           ← 401 JSON responses
    │   │   ├── UserDetailsImpl.java             ← Spring Security UserDetails
    │   │   └── UserDetailsServiceImpl.java      ← DB user loader
    │   └── service/impl/
    │       ├── AuthServiceImpl.java             ← Login, Register, Refresh, Logout
    │       ├── ReportServiceImpl.java           ← Core report + RBAC logic
    │       ├── DoctorServiceImpl.java           ← Doctor CRUD
    │       ├── PatientServiceImpl.java          ← Patient CRUD
    │       ├── CloudinaryServiceImpl.java       ← Image upload/delete
    │       ├── EmailServiceImpl.java            ← HTML email sending
    │       └── PdfServiceImpl.java              ← iText PDF generation
    └── resources/
        └── application.properties
```

---

## STEP 1 — Prerequisites

Install the following before starting:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| MySQL | 8.0+ | https://dev.mysql.com/downloads |
| Git | Any | https://git-scm.com |

Verify installations:
```bash
java --version       # must be 17+
mvn --version        # must be 3.8+
mysql --version      # must be 8.0+
```

---

## STEP 2 — MySQL Database Setup

Open MySQL CLI or MySQL Workbench:
```sql
CREATE DATABASE orthocarechain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'orthouser'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON orthocarechain.* TO 'orthouser'@'localhost';
FLUSH PRIVILEGES;
```

Tables are **auto-created** by Hibernate on first startup (ddl-auto=update).

---

## STEP 3 — Configure application.properties

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/orthocarechain?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=orthouser
spring.datasource.password=YourStrongPassword123!

# JWT — use a strong 64+ character secret
jwt.secret=OrthoCareChainSuperSecretKey2024!@#$%^&*()_+SuperLongKeyForHS512Algorithm
jwt.expiration.ms=86400000           # 24 hours
jwt.refresh.expiration.ms=604800000  # 7 days

# Cloudinary — get from https://cloudinary.com/console
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

# Gmail — use App Password from Google Account → Security → App Passwords
spring.mail.username=your_email@gmail.com
spring.mail.password=your_16_char_app_password
```

---

## STEP 4 — Cloudinary Setup

1. Create a free account at https://cloudinary.com
2. Dashboard → copy **Cloud Name, API Key, API Secret**
3. Paste into `application.properties`

---

## STEP 5 — Gmail SMTP Setup

1. Go to Google Account → Security → 2-Step Verification (enable it)
2. Security → App Passwords → Select "Mail" → Generate
3. Copy the 16-character password into `spring.mail.password`

---

## STEP 6 — Build and Run

```bash
# Clone or navigate to project root
cd orthocarechain

# Build (skip tests for first run)
mvn clean install -DskipTests

# Run
mvn spring-boot:run
```

Application starts at: **http://localhost:8080/api**

On first startup, you will see in logs:
```
Default Admin account created:
  Username: admin
  Password: Admin@12345
  *** CHANGE THIS PASSWORD IMMEDIATELY IN PRODUCTION ***
```

---

## STEP 7 — API Reference & Role-Based Access Control

### Authentication Endpoints (PUBLIC — No token needed)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login → returns JWT + refresh token |
| POST | `/api/auth/register` | Register as DOCTOR, PATIENT, or PHARMACY |
| POST | `/api/auth/refresh-token` | Get new access token using refresh token |
| POST | `/api/auth/logout` | Invalidate refresh token (**requires auth**) |
| GET | `/api/auth/me` | Get current user info (**requires auth**) |

### Login Request Example
```json
POST /api/auth/login
{
    "usernameOrEmail": "admin",
    "password": "Admin@12345"
}
```

### Register Request Example
```json
POST /api/auth/register
{
    "username": "drsmith",
    "email": "dr.smith@hospital.com",
    "password": "Doctor@123",
    "role": "ROLE_DOCTOR",
    "firstName": "John",
    "lastName": "Smith",
    "phone": "+1234567890"
}
```

### Using JWT Token
Include in every protected request:
```
Authorization: Bearer <your_access_token_here>
```

---

## RBAC Matrix — Who Can Do What

| Endpoint | ADMIN | DOCTOR | PATIENT | PHARMACY |
|----------|-------|--------|---------|----------|
| Create report | ✅ | ✅ (own) | ❌ | ❌ |
| View any report | ✅ | ✅ (own) | ✅ (own) | ❌ |
| Update report | ✅ | ✅ (own) | ❌ | ❌ |
| Delete report | ✅ | ✅ (own) | ❌ | ❌ |
| Download PDF | ✅ | ✅ (own) | ✅ (own) | ❌ |
| Upload images | ✅ | ✅ (own) | ❌ | ❌ |
| View prescriptions | ✅ | ✅ | ✅ (own) | ✅ |
| Add prescription | ✅ | ✅ (own) | ❌ | ❌ |
| View patient list | ✅ | ✅ | ❌ | ❌ |
| View doctor list | ✅ | ✅ | ✅ | ❌ |
| Manage users | ✅ | ❌ | ❌ | ❌ |
| Create admin | ✅ | ❌ | ❌ | ❌ |
| System stats | ✅ | ❌ | ❌ | ❌ |

---

## Doctor Profile Setup (POST login as DOCTOR)

```json
POST /api/doctors/profile
Authorization: Bearer <doctor_token>

{
    "medicalLicenseNumber": "MED-2024-001",
    "specialization": "Orthopedics",
    "hospital": "City General Hospital",
    "department": "Orthopedic Surgery",
    "yearsOfExperience": 10,
    "bio": "Specialist in joint replacement and sports injuries."
}
```

---

## Patient Profile Setup (POST login as PATIENT)

```json
POST /api/patients/profile
Authorization: Bearer <patient_token>

{
    "dateOfBirth": "1990-05-15",
    "gender": "MALE",
    "bloodGroup": "O+",
    "height": 175.0,
    "weight": 70.0,
    "allergies": "Penicillin",
    "chronicConditions": "None",
    "emergencyContactName": "Jane Doe",
    "emergencyContactPhone": "+1234567890",
    "address": "123 Main St, Springfield"
}
```

---

## Create a Visit Report (DOCTOR only)

```json
POST /api/reports
Authorization: Bearer <doctor_token>

{
    "patientId": 1,
    "visitDate": "2026-02-16",
    "nextVisitDate": "2026-03-16",
    "diagnosis": "Lumbar Disc Herniation",
    "diagnosisDetails": "L4-L5 disc herniation with nerve compression.",
    "severityLevel": "MODERATE",
    "doctorNotes": "Patient reports radiating pain down left leg.",
    "treatmentPlan": "Physical therapy + anti-inflammatory medication.",
    "followUpInstructions": "Avoid heavy lifting. Return if pain worsens.",
    "prescriptions": [
        {
            "drugName": "Ibuprofen",
            "dosage": "400mg",
            "frequency": "Twice daily",
            "duration": "14 days",
            "instructions": "Take with food",
            "medicationStartDate": "2026-02-16",
            "medicationEndDate": "2026-03-01"
        }
    ]
}
```

---

## Upload Medical Image (DOCTOR only)

```
POST /api/images/upload/{reportId}
Authorization: Bearer <doctor_token>
Content-Type: multipart/form-data

file=<image_file>
imageType=XRAY
description=AP view of lumbar spine
bodyPart=Lower Back
```

---

## Download PDF Report

```
GET /api/reports/{reportId}/download-pdf
Authorization: Bearer <doctor_or_patient_token>
```
Returns: `application/pdf` binary

---

## STEP 8 — Scheduler Configuration

The scheduler sends emails at **8:00 AM daily** (configurable):
```properties
scheduler.email.reminder.cron=0 0 8 * * ?
```

CRON format: `seconds minutes hours day-of-month month day-of-week`

Examples:
- `0 0 8 * * ?` — Every day at 8 AM
- `0 0 */6 * * ?` — Every 6 hours
- `0 */30 * * * ?` — Every 30 minutes

---

## STEP 9 — Common Errors & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Access denied` | Missing `Authorization` header | Add `Bearer <token>` header |
| `403 Forbidden` | Wrong role for endpoint | Check RBAC matrix above |
| `401 Unauthorized` | Expired token | Use `/auth/refresh-token` |
| `Could not connect to MySQL` | DB not running | Start MySQL: `sudo service mysql start` |
| `Failed to upload image` | Bad Cloudinary credentials | Check `application.properties` |
| `Mail send failure` | Wrong Gmail app password | Regenerate in Google Account settings |

---

## STEP 10 — Production Checklist

- [ ] Change default admin password immediately
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (not update)
- [ ] Use environment variables for secrets (never commit credentials)
- [ ] Enable HTTPS (use a reverse proxy like Nginx)
- [ ] Set strong JWT secret (64+ chars)
- [ ] Configure CORS to allow only your frontend domain
- [ ] Enable MySQL SSL connection
- [ ] Set up database backups

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 3.2.3 |
| Language | Java 17 |
| Database | MySQL 8 + JPA/Hibernate |
| Authentication | JWT (HS512) + Spring Security 6 |
| Image Storage | Cloudinary |
| Email | Spring Mail + SMTP |
| PDF Generation | iText 5 |
| Build Tool | Maven |
| Password Hashing | BCrypt (strength 12) |
| Session | Stateless (JWT) |
