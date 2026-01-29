# Enterprise Data Sharing Application

A Spring Boot 3.x application demonstrating enterprise-grade access control mechanisms including RBAC (Role-Based Access Control), ABAC (Attribute-Based Access Control), and CBAC (Context-Based Access Control), with Keycloak authentication and comprehensive audit logging.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Request                               │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Keycloak (Authentication)                         │
│  - JWT Token Generation                                              │
│  - User Attributes in Claims                                         │
│  - Realm Roles                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Security Filter Chain                      │
│  - JWT Validation                                                    │
│  - Custom Authentication Converter                                   │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Access Control Service                            │
├─────────────────────────────────────────────────────────────────────┤
│  1. RBAC Check     │ Role + Organization Level Hierarchy            │
│  2. ABAC Check     │ Clearance vs Sensitivity + Custom Rules        │
│  3. CBAC Check     │ IP Range + Business Hours                      │
│  4. Row-Level      │ Specific Record Access Rules                   │
│  5. Column-Level   │ Field Visibility Filtering                     │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Audit Service                                     │
│  - All access decisions logged                                       │
│  - Field-level change tracking                                       │
│  - Correlation ID tracing                                            │
└─────────────────────────────────────────────────────────────────────┘
```

## Organization Hierarchy

```
EXECUTIVE (Top Secret Clearance)
    │
    ├── DEPARTMENT (Secret Clearance)
    │       │
    │       ├── TEAM (Confidential Clearance)
    │       │      │
    │       │      └── INDIVIDUAL (Internal Clearance)
    │       │
    │       └── TEAM
    │              │
    │              └── INDIVIDUAL
    │
    └── DEPARTMENT
            │
            └── TEAM
                   │
                   └── INDIVIDUAL
```

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Maven 3.9+ (optional, wrapper included)

### Start with Docker Compose

```bash
# Clone and navigate to project
cd enterprise-data-sharing

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app
```

### Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| API | http://localhost:8080 | - |
| Keycloak Admin | http://localhost:8180 | admin / admin |
| pgAdmin (optional) | http://localhost:5050 | admin@enterprise.com / admin |

To start pgAdmin:
```bash
docker-compose --profile tools up -d pgadmin
```

## Test Users

| Username | Password | Role | Department | Clearance |
|----------|----------|------|------------|-----------|
| admin | admin123 | ADMIN | - | TOP_SECRET |
| john.ceo | exec123 | EXECUTIVE | EXECUTIVE | TOP_SECRET |
| sarah.engineering | dept123 | DEPARTMENT_HEAD | ENGINEERING | SECRET |
| mike.sales | dept123 | DEPARTMENT_HEAD | SALES | SECRET |
| lisa.hr | dept123 | DEPARTMENT_HEAD | HR | SECRET |
| alice.backend | team123 | TEAM_LEAD | ENGINEERING/BACKEND | CONFIDENTIAL |
| bob.frontend | team123 | TEAM_LEAD | ENGINEERING/FRONTEND | CONFIDENTIAL |
| dev.one | emp123 | EMPLOYEE | ENGINEERING/BACKEND | INTERNAL |
| dev.two | emp123 | EMPLOYEE | ENGINEERING/BACKEND | INTERNAL |
| auditor | audit123 | AUDITOR | - | SECRET |

## API Endpoints

### Authentication

Get a JWT token from Keycloak:

```bash
# Get token for a user
TOKEN=$(curl -s -X POST \
  "http://localhost:8180/realms/enterprise/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=datasharing-web" \
  -d "username=john.ceo" \
  -d "password=exec123" | jq -r '.access_token')

echo $TOKEN
```

### Data Operations

```bash
# Create data
curl -X POST http://localhost:8080/api/v1/data \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q1 Report",
    "date": "2024-01-15",
    "data": "Quarterly performance summary",
    "sensitivityLevel": "CONFIDENTIAL",
    "organizationLevel": "DEPARTMENT"
  }'

# Get data by ID
curl -X GET http://localhost:8080/api/v1/data/1 \
  -H "Authorization: Bearer $TOKEN"

# List all accessible data
curl -X GET http://localhost:8080/api/v1/data \
  -H "Authorization: Bearer $TOKEN"

# Get my data only
curl -X GET http://localhost:8080/api/v1/data/my \
  -H "Authorization: Bearer $TOKEN"

# Update data
curl -X PUT http://localhost:8080/api/v1/data/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Q1 Report - Updated",
    "data": "Updated quarterly performance summary"
  }'

# Delete data (soft delete)
curl -X DELETE http://localhost:8080/api/v1/data/1 \
  -H "Authorization: Bearer $TOKEN"

# Search data
curl -X GET "http://localhost:8080/api/v1/data/search?q=Report" \
  -H "Authorization: Bearer $TOKEN"
```

### Admin Operations (requires ADMIN role)

```bash
# Get admin token
ADMIN_TOKEN=$(curl -s -X POST \
  "http://localhost:8180/realms/enterprise/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=datasharing-web" \
  -d "username=admin" \
  -d "password=admin123" | jq -r '.access_token')

# List access control rules
curl -X GET http://localhost:8080/api/v1/admin/access-rules \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Create access rule
curl -X POST http://localhost:8080/api/v1/admin/access-rules \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleName": "Custom Rule",
    "description": "Allow SALES to read specific columns",
    "principalType": "DEPARTMENT",
    "principalValue": "SALES",
    "canRead": true,
    "visibleColumns": ["id", "name", "date", "data"]
  }'

# View audit logs
curl -X GET "http://localhost:8080/api/v1/admin/audit?action=READ" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Get audit statistics
curl -X GET http://localhost:8080/api/v1/admin/audit/stats \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# List user attributes
curl -X GET http://localhost:8080/api/v1/admin/user-attributes \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Access Control Details

### RBAC (Role-Based Access Control)

Roles determine base permissions:

| Role | Permissions |
|------|-------------|
| ADMIN | Full access to all operations |
| EXECUTIVE | Read all data, write to executive-level |
| DEPARTMENT_HEAD | Read department data, write to department-level |
| TEAM_LEAD | Read team data, write to team-level |
| EMPLOYEE | Read/write own data only |
| AUDITOR | Read-only access to audit logs |
| DATA_STEWARD | Manage access control rules |

### ABAC (Attribute-Based Access Control)

Attributes provide fine-grained control:

- **Clearance Level**: PUBLIC < INTERNAL < CONFIDENTIAL < SECRET < TOP_SECRET
- **Department**: Engineering, Sales, HR, etc.
- **Team**: Backend, Frontend, Enterprise, etc.
- **Manager Status**: is_manager, is_department_head, is_executive

### CBAC (Context-Based Access Control)

Environmental factors:

- **IP Range Validation**: Restrict access to allowed networks
- **Business Hours**: Limit sensitive data access to work hours
- **Request Context**: User agent, correlation ID tracking

### Row-Level Security

Specific records can have access rules:

```json
{
  "ruleName": "Strategic Plan Access",
  "dataId": 1,
  "principalType": "ORGANIZATION",
  "principalValue": "EXECUTIVE",
  "canRead": true,
  "canUpdate": true,
  "canDelete": false
}
```

### Column-Level Security

Fields visible based on clearance:

| Field | Required Clearance |
|-------|-------------------|
| id, name, date, data | PUBLIC |
| metadata | INTERNAL |
| confidentialNotes | CONFIDENTIAL |
| financialData | SECRET |

## Data Visibility by Level

| User Level | Can See |
|------------|---------|
| Executive | All organization data |
| Department Head | Department + Team + Individual data |
| Team Lead | Team + Individual data |
| Individual | Own data only |

## Audit Logging

Every operation is logged with:

- User identity and roles
- Action performed (CREATE, READ, UPDATE, DELETE)
- Access decision (GRANTED, DENIED_*)
- RBAC roles evaluated
- ABAC attributes checked
- CBAC context (IP, time, etc.)
- Row-level and column-level decisions
- Before/after values for updates
- Correlation ID for request tracing
- Data hash for integrity

## Project Structure

```
enterprise-data-sharing/
├── docker-compose.yml          # Container orchestration
├── Dockerfile                  # Application container
├── pom.xml                     # Maven dependencies
├── db/
│   └── init/
│       └── 01-init-schema.sql  # Database schema + sample data
├── keycloak/
│   └── realm-export.json       # Keycloak configuration
└── src/main/java/com/enterprise/datasharing/
    ├── DataSharingApplication.java
    ├── config/
    │   └── SecurityConfig.java
    ├── controller/
    │   ├── MyDataController.java
    │   └── AdminController.java
    ├── dto/
    │   └── MyDataDto.java
    ├── entity/
    │   ├── MyData.java
    │   ├── AuditLog.java
    │   ├── DataAccessControl.java
    │   └── UserAttribute.java
    ├── exception/
    │   ├── AccessDeniedException.java
    │   ├── ResourceNotFoundException.java
    │   └── GlobalExceptionHandler.java
    ├── repository/
    │   ├── MyDataRepository.java
    │   ├── AuditLogRepository.java
    │   ├── DataAccessControlRepository.java
    │   └── UserAttributeRepository.java
    ├── security/
    │   ├── AccessControlService.java
    │   ├── AccessDecision.java
    │   ├── CustomJwtAuthenticationConverter.java
    │   └── SecurityContext.java
    └── service/
        ├── MyDataService.java
        └── AuditService.java
```

## Development

### Local Development

```bash
# Start only infrastructure
docker-compose up -d postgres keycloak

# Run application locally
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Building

```bash
# Build JAR
./mvnw clean package -DskipTests

# Build Docker image
docker build -t datasharing-app .
```

### Testing Access Control

Test different access scenarios:

```bash
# 1. Executive can see all data including restricted
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/enterprise/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=datasharing-web&username=john.ceo&password=exec123" | jq -r '.access_token')
curl -s http://localhost:8080/api/v1/data | jq

# 2. Department head sees department data only
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/enterprise/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=datasharing-web&username=sarah.engineering&password=dept123" | jq -r '.access_token')
curl -s http://localhost:8080/api/v1/data -H "Authorization: Bearer $TOKEN" | jq

# 3. Individual sees own data only
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/enterprise/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=datasharing-web&username=dev.one&password=emp123" | jq -r '.access_token')
curl -s http://localhost:8080/api/v1/data -H "Authorization: Bearer $TOKEN" | jq

# 4. Column filtering - dev.one won't see financialData
curl -s http://localhost:8080/api/v1/data/1 -H "Authorization: Bearer $TOKEN" | jq
```

## Configuration

Key configuration options in `application.yml`:

```yaml
access-control:
  rbac:
    enabled: true
  abac:
    enabled: true
  cbac:
    enabled: true
    allowed-ip-ranges:
      - "10.0.0.0/8"
      - "192.168.0.0/16"
    business-hours:
      enabled: true
      start: "08:00"
      end: "18:00"
      timezone: "America/New_York"
  row-level:
    enabled: true
  column-level:
    enabled: true

audit:
  enabled: true
  log-sensitive-data: false
  retention-days: 365
```

## Security Considerations

1. **JWT Tokens**: Short-lived (1 hour), validated against Keycloak
2. **Password Security**: Use strong passwords in production
3. **Network Security**: Configure IP allowlists appropriately
4. **Audit Trail**: Immutable audit logs for compliance
5. **Soft Delete**: Data is never truly deleted
6. **Field-Level Encryption**: Consider for sensitive columns

## Troubleshooting

### Common Issues

1. **Keycloak not ready**: Wait for health check, may take 60+ seconds
2. **Token expired**: Get a new token from Keycloak
3. **Access denied**: Check user's roles and clearance level
4. **Database errors**: Ensure PostgreSQL is running and healthy

### Logs

```bash
# Application logs
docker-compose logs -f app

# Keycloak logs
docker-compose logs -f keycloak

# Database logs
docker-compose logs -f postgres
```

## License

MIT License - See LICENSE file for details.
