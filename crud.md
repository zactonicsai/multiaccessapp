# CRUD Operations Access Control Guide
## How Create, Read, Update, and Delete Operations Are Controlled

**Document Classification:** Technical Reference  
**Version:** 1.0  
**Date:** January 2026  

---

## Table of Contents

1. [Overview](#overview)
2. [CRUD Control Matrix](#crud-control-matrix)
3. [How Metadata Enforces Access Control](#how-metadata-enforces-access-control)
4. [CREATE Operations](#create-operations)
5. [READ Operations](#read-operations)
6. [UPDATE Operations](#update-operations)
7. [DELETE Operations](#delete-operations)
8. [API Request/Response Examples](#api-requestresponse-examples)
9. [Error Handling & Denial Responses](#error-handling--denial-responses)
10. [Audit Trail for CRUD Operations](#audit-trail-for-crud-operations)

---

## Overview

Every CRUD operation in the Enterprise Data Sharing Platform passes through a **multi-layer security gate** before execution. The system uses **metadata** embedded in both the request and the data itself to make access decisions.

### The Security Gate Model

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CRUD OPERATION FLOW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   USER REQUEST                                                              │
│       │                                                                      │
│       ▼                                                                      │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ GATE 1: AUTHENTICATION                                              │   │
│   │ "Is this a valid, authenticated user?"                              │   │
│   │                                                                     │   │
│   │ Metadata Used:                                                      │   │
│   │ • JWT Token validity                                                │   │
│   │ • Token expiration                                                  │   │
│   │ • Token signature verification                                      │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ GATE 2: OPERATION AUTHORIZATION                                     │   │
│   │ "Can this user perform this type of operation?"                     │   │
│   │                                                                     │   │
│   │ Metadata Used:                                                      │   │
│   │ • User roles (from JWT)                                             │   │
│   │ • Operation type (CREATE/READ/UPDATE/DELETE)                        │   │
│   │ • Client permissions (from Keycloak)                                │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ GATE 3: DATA-LEVEL AUTHORIZATION                                    │   │
│   │ "Can this user access THIS SPECIFIC data?"                          │   │
│   │                                                                     │   │
│   │ Metadata Used:                                                      │   │
│   │ • User clearance level                                              │   │
│   │ • User organization level                                           │   │
│   │ • User department/team                                              │   │
│   │ • Data sensitivity level                                            │   │
│   │ • Data organization level                                           │   │
│   │ • Data owner department/team                                        │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ GATE 4: CONTEXT VERIFICATION                                        │   │
│   │ "Is the request context appropriate?"                               │   │
│   │                                                                     │   │
│   │ Metadata Used:                                                      │   │
│   │ • Source IP address                                                 │   │
│   │ • Request timestamp                                                 │   │
│   │ • Network zone                                                      │   │
│   │ • User agent/device info                                            │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ GATE 5: FIELD-LEVEL FILTERING                                       │   │
│   │ "Which specific fields can be accessed?"                            │   │
│   │                                                                     │   │
│   │ Metadata Used:                                                      │   │
│   │ • Field sensitivity mappings                                        │   │
│   │ • User clearance for field access                                   │   │
│   │ • Column-level access rules                                         │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                      │
│       ▼                                                                      │
│   OPERATION EXECUTED (with filtering applied)                               │
│       │                                                                      │
│       ▼                                                                      │
│   AUDIT LOG CREATED                                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## CRUD Control Matrix

### By Organization Level

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CRUD PERMISSIONS BY ORGANIZATION LEVEL                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  USER LEVEL        │ DATA LEVEL    │ CREATE │ READ │ UPDATE │ DELETE        │
│  ══════════════════╪═══════════════╪════════╪══════╪════════╪═══════        │
│                    │               │        │      │        │               │
│  EXECUTIVE         │ EXECUTIVE     │   ✓    │  ✓   │   ✓    │   ✓          │
│                    │ DEPARTMENT    │   ✓    │  ✓   │   ✓    │   ✓          │
│                    │ TEAM          │   ✓    │  ✓   │   ✓    │   ✓          │
│                    │ INDIVIDUAL    │   ✓    │  ✓   │   ✓    │   ✓          │
│  ──────────────────┼───────────────┼────────┼──────┼────────┼───────        │
│  DEPARTMENT_HEAD   │ EXECUTIVE     │   ✗    │  ✗   │   ✗    │   ✗          │
│                    │ DEPARTMENT*   │   ✓    │  ✓   │   ✓    │   ✓          │
│                    │ TEAM*         │   ✓    │  ✓   │   ✓    │   ✓          │
│                    │ INDIVIDUAL*   │   ✓    │  ✓   │   ✓    │   ✓          │
│  ──────────────────┼───────────────┼────────┼──────┼────────┼───────        │
│  TEAM_LEAD         │ EXECUTIVE     │   ✗    │  ✗   │   ✗    │   ✗          │
│                    │ DEPARTMENT    │   ✗    │  ✗   │   ✗    │   ✗          │
│                    │ TEAM*         │   ✓    │  ✓   │   ✓    │   ✓          │
│                    │ INDIVIDUAL*   │   ✓    │  ✓   │   ✓    │   ✓          │
│  ──────────────────┼───────────────┼────────┼──────┼────────┼───────        │
│  INDIVIDUAL        │ EXECUTIVE     │   ✗    │  ✗   │   ✗    │   ✗          │
│                    │ DEPARTMENT    │   ✗    │  ✗   │   ✗    │   ✗          │
│                    │ TEAM          │   ✗    │  ✗   │   ✗    │   ✗          │
│                    │ INDIVIDUAL**  │   ✓    │  ✓   │   ✓    │   ✓          │
│                                                                              │
│  * Within their own department/team only                                    │
│  ** Own data only                                                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### By Clearance Level (Sensitivity)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CRUD PERMISSIONS BY CLEARANCE LEVEL                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  USER CLEARANCE    │ DATA SENSITIVITY │ CREATE │ READ │ UPDATE │ DELETE     │
│  ══════════════════╪══════════════════╪════════╪══════╪════════╪═══════     │
│                    │                  │        │      │        │            │
│  TOP_SECRET        │ RESTRICTED       │   ✓    │  ✓   │   ✓    │   ✓       │
│                    │ CONFIDENTIAL     │   ✓    │  ✓   │   ✓    │   ✓       │
│                    │ INTERNAL         │   ✓    │  ✓   │   ✓    │   ✓       │
│                    │ PUBLIC           │   ✓    │  ✓   │   ✓    │   ✓       │
│  ──────────────────┼──────────────────┼────────┼──────┼────────┼───────     │
│  SECRET            │ RESTRICTED       │   ✗    │  ✗   │   ✗    │   ✗       │
│                    │ CONFIDENTIAL     │   ✓    │  ✓   │   ✓    │   ✓       │
│                    │ INTERNAL         │   ✓    │  ✓   │   ✓    │   ✓       │
│                    │ PUBLIC           │   ✓    │  ✓   │   ✓    │   ✓       │
│  ──────────────────┼──────────────────┼────────┼──────┼────────┼───────     │
│  CONFIDENTIAL      │ RESTRICTED       │   ✗    │  ✗   │   ✗    │   ✗       │
│                    │ CONFIDENTIAL     │   ✓    │  ✓   │   ✓    │   ✓       │
│                    │ INTERNAL         │   ✓    │  ✓   │   ✓    │   ✓       │
│                    │ PUBLIC           │   ✓    │  ✓   │   ✓    │   ✓       │
│  ──────────────────┼──────────────────┼────────┼──────┼────────┼───────     │
│  INTERNAL          │ RESTRICTED       │   ✗    │  ✗   │   ✗    │   ✗       │
│                    │ CONFIDENTIAL     │   ✗    │  ✗   │   ✗    │   ✗       │
│                    │ INTERNAL         │   ✓    │  ✓   │   ✓    │   ✓       │
│                    │ PUBLIC           │   ✓    │  ✓   │   ✓    │   ✓       │
│  ──────────────────┼──────────────────┼────────┼──────┼────────┼───────     │
│  PUBLIC            │ RESTRICTED       │   ✗    │  ✗   │   ✗    │   ✗       │
│                    │ CONFIDENTIAL     │   ✗    │  ✗   │   ✗    │   ✗       │
│                    │ INTERNAL         │   ✗    │  ✗   │   ✗    │   ✗       │
│                    │ PUBLIC           │   ✓    │  ✓   │   ✓    │   ✓       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Combined Matrix (Real-World Scenarios)

| User | Org Level | Clearance | Can CREATE | Can READ | Can UPDATE | Can DELETE |
|------|-----------|-----------|------------|----------|------------|------------|
| CEO | EXECUTIVE | TOP_SECRET | All data | All data | All data | All data |
| Dept Head | DEPARTMENT | SECRET | Dept data at CONFIDENTIAL or below | Dept data at CONFIDENTIAL or below | Dept data at CONFIDENTIAL or below | Dept data at CONFIDENTIAL or below |
| Team Lead | TEAM | CONFIDENTIAL | Team data at CONFIDENTIAL or below | Team data at CONFIDENTIAL or below | Team data at CONFIDENTIAL or below | Team data at CONFIDENTIAL or below |
| Developer | INDIVIDUAL | INTERNAL | Own data at INTERNAL or below | Own data at INTERNAL or below | Own data at INTERNAL or below | Own data at INTERNAL or below |

---

## How Metadata Enforces Access Control

### Metadata in JWT Token (User Context)

Every API request includes a JWT token containing user metadata:

```json
{
  "sub": "dept-eng-001",
  "name": "Sarah Engineering",
  "email": "sarah.engineering@enterprise.com",
  
  "realm_access": {
    "roles": ["DEPARTMENT_HEAD", "EMPLOYEE"]
  },
  
  "resource_access": {
    "datasharing-api": {
      "roles": ["data:read", "data:write"]
    }
  },
  
  "user_id": "dept-eng-001",
  "department": "ENGINEERING",
  "team": "MANAGEMENT",
  "clearance_level": "SECRET",
  "organization_level": "DEPARTMENT",
  "is_manager": true,
  "is_department_head": true,
  "is_executive": false,
  
  "iat": 1705329600,
  "exp": 1705333200,
  "iss": "http://keycloak:8080/realms/enterprise"
}
```

### Metadata in Data Records

Each data record contains metadata used for access decisions:

```json
{
  "id": 12345,
  "name": "Q3 Engineering Report",
  "date": "2024-10-15",
  "data": "Quarterly performance summary...",
  
  "_metadata": {
    "sensitivity_level": "CONFIDENTIAL",
    "organization_level": "DEPARTMENT",
    "owner_id": "dept-eng-001",
    "owner_department": "ENGINEERING",
    "owner_team": "MANAGEMENT",
    
    "created_at": "2024-10-01T09:00:00Z",
    "created_by": "dept-eng-001",
    "updated_at": "2024-10-15T14:30:00Z",
    "updated_by": "dept-eng-001",
    "version": 3
  },
  
  "_field_sensitivity": {
    "name": "PUBLIC",
    "date": "PUBLIC",
    "data": "INTERNAL",
    "confidential_notes": "CONFIDENTIAL",
    "financial_data": "SECRET",
    "executive_comments": "RESTRICTED"
  }
}
```

### Access Decision Logic

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ACCESS DECISION ALGORITHM                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  function checkAccess(user, operation, data):                               │
│                                                                              │
│    // STEP 1: Check Organization Level Hierarchy                            │
│    if user.organization_level < data.organization_level:                    │
│      return DENIED_ROLE                                                     │
│                                                                              │
│    // STEP 2: Check Department/Team Scope (if not executive)                │
│    if user.organization_level != EXECUTIVE:                                 │
│      if user.organization_level == DEPARTMENT:                              │
│        if user.department != data.owner_department:                         │
│          return DENIED_ROLE                                                 │
│      if user.organization_level == TEAM:                                    │
│        if user.department != data.owner_department OR                       │
│           user.team != data.owner_team:                                     │
│          return DENIED_ROLE                                                 │
│      if user.organization_level == INDIVIDUAL:                              │
│        if user.user_id != data.owner_id:                                    │
│          return DENIED_ROLE                                                 │
│                                                                              │
│    // STEP 3: Check Clearance vs Sensitivity                                │
│    if user.clearance_level < data.sensitivity_level:                        │
│      return DENIED_ATTRIBUTE                                                │
│                                                                              │
│    // STEP 4: Check Context (IP, Time, etc.)                                │
│    if not isValidContext(user.request_context):                             │
│      return DENIED_CONTEXT                                                  │
│                                                                              │
│    // STEP 5: Check Row-Level Rules                                         │
│    if hasBlockingRowRule(data.id, user):                                    │
│      return DENIED_ROW_LEVEL                                                │
│                                                                              │
│    // STEP 6: Calculate Visible Fields                                      │
│    visible_fields = getVisibleFields(user.clearance_level, data)            │
│                                                                              │
│    return GRANTED with visible_fields                                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## CREATE Operations

### What Happens When a User Creates Data

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CREATE OPERATION FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  POST /api/v1/data                                                          │
│  Authorization: Bearer <jwt_token>                                          │
│  Content-Type: application/json                                             │
│                                                                              │
│  Request Body:                                                              │
│  {                                                                          │
│    "name": "New Project Plan",                                              │
│    "date": "2024-01-20",                                                    │
│    "data": "Project details...",                                            │
│    "sensitivityLevel": "CONFIDENTIAL",    ◄── User specifies sensitivity   │
│    "organizationLevel": "TEAM"            ◄── User specifies org level     │
│  }                                                                          │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ VALIDATION CHECKS                                                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ 1. CAN USER CREATE AT THIS ORGANIZATION LEVEL?                      │   │
│  │    ────────────────────────────────────────────                     │   │
│  │    Rule: User can only create at or below their organization level  │   │
│  │                                                                     │   │
│  │    User Org Level     Can Create At                                 │   │
│  │    ─────────────────  ──────────────────────────────────            │   │
│  │    EXECUTIVE          EXECUTIVE, DEPARTMENT, TEAM, INDIVIDUAL       │   │
│  │    DEPARTMENT         DEPARTMENT, TEAM, INDIVIDUAL                  │   │
│  │    TEAM               TEAM, INDIVIDUAL                              │   │
│  │    INDIVIDUAL         INDIVIDUAL only                               │   │
│  │                                                                     │   │
│  │ 2. CAN USER CREATE AT THIS SENSITIVITY LEVEL?                       │   │
│  │    ────────────────────────────────────────────                     │   │
│  │    Rule: User can only create data at or below their clearance      │   │
│  │                                                                     │   │
│  │    User Clearance     Can Create Sensitivity                        │   │
│  │    ─────────────────  ──────────────────────────────────            │   │
│  │    TOP_SECRET         RESTRICTED, CONFIDENTIAL, INTERNAL, PUBLIC    │   │
│  │    SECRET             CONFIDENTIAL, INTERNAL, PUBLIC                │   │
│  │    CONFIDENTIAL       CONFIDENTIAL, INTERNAL, PUBLIC                │   │
│  │    INTERNAL           INTERNAL, PUBLIC                              │   │
│  │    PUBLIC             PUBLIC only                                   │   │
│  │                                                                     │   │
│  │ 3. ARE SENSITIVE FIELDS PERMITTED?                                  │   │
│  │    ─────────────────────────────────                                │   │
│  │    If request includes confidential_notes:                          │   │
│  │      → Requires CONFIDENTIAL clearance                              │   │
│  │    If request includes financial_data:                              │   │
│  │      → Requires SECRET clearance                                    │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ METADATA AUTO-POPULATED ON CREATE                                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ The system automatically sets:                                      │   │
│  │                                                                     │   │
│  │ • owner_id         = user.user_id (from JWT)                        │   │
│  │ • owner_department = user.department (from JWT)                     │   │
│  │ • owner_team       = user.team (from JWT)                           │   │
│  │ • created_at       = current timestamp                              │   │
│  │ • created_by       = user.user_id                                   │   │
│  │ • version          = 1                                              │   │
│  │                                                                     │   │
│  │ User CANNOT override these fields - they are set from JWT metadata  │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### CREATE Prevention Examples

| User | Clearance | Org Level | Attempted Create | Result | Reason |
|------|-----------|-----------|------------------|--------|--------|
| dev.one | INTERNAL | INDIVIDUAL | CONFIDENTIAL, TEAM | **DENIED** | Cannot create above clearance OR org level |
| dev.one | INTERNAL | INDIVIDUAL | INTERNAL, INDIVIDUAL | **ALLOWED** | Matches user's level |
| alice.backend | CONFIDENTIAL | TEAM | CONFIDENTIAL, DEPARTMENT | **DENIED** | Cannot create at DEPARTMENT level |
| alice.backend | CONFIDENTIAL | TEAM | CONFIDENTIAL, TEAM | **ALLOWED** | Matches clearance and org level |
| sarah.engineering | SECRET | DEPARTMENT | RESTRICTED, DEPARTMENT | **DENIED** | RESTRICTED requires TOP_SECRET |
| john.ceo | TOP_SECRET | EXECUTIVE | RESTRICTED, EXECUTIVE | **ALLOWED** | Full permissions |

---

## READ Operations

### What Happens When a User Reads Data

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          READ OPERATION FLOW                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  GET /api/v1/data/12345                                                     │
│  Authorization: Bearer <jwt_token>                                          │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 1: FETCH DATA WITH METADATA                                     │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ Database returns full record:                                       │   │
│  │ {                                                                   │   │
│  │   "id": 12345,                                                      │   │
│  │   "name": "Q3 Strategic Plan",                                      │   │
│  │   "sensitivity_level": "CONFIDENTIAL",                              │   │
│  │   "organization_level": "DEPARTMENT",                               │   │
│  │   "owner_department": "ENGINEERING",                                │   │
│  │   "owner_team": "MANAGEMENT",                                       │   │
│  │   "owner_id": "dept-eng-001",                                       │   │
│  │   "confidential_notes": "Merger discussions...",                    │   │
│  │   "financial_data": {"budget": 5000000},                            │   │
│  │   ...                                                               │   │
│  │ }                                                                   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 2: COMPARE USER METADATA VS DATA METADATA                       │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ User (from JWT)              Data (from record)                     │   │
│  │ ────────────────────         ─────────────────────                  │   │
│  │ org_level: DEPARTMENT        org_level: DEPARTMENT     ✓ MATCH     │   │
│  │ department: ENGINEERING      owner_dept: ENGINEERING   ✓ MATCH     │   │
│  │ clearance: SECRET            sensitivity: CONFIDENTIAL ✓ SUFFICIENT│   │
│  │                                                                     │   │
│  │ RBAC Check: PASSED                                                  │   │
│  │ ABAC Check: PASSED                                                  │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 3: APPLY COLUMN-LEVEL FILTERING                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ Field                  Required Clearance    User Has    Visible?   │   │
│  │ ─────────────────────  ──────────────────    ─────────   ────────   │   │
│  │ id                     PUBLIC                SECRET      ✓ YES      │   │
│  │ name                   PUBLIC                SECRET      ✓ YES      │   │
│  │ date                   PUBLIC                SECRET      ✓ YES      │   │
│  │ data                   INTERNAL              SECRET      ✓ YES      │   │
│  │ confidential_notes     CONFIDENTIAL          SECRET      ✓ YES      │   │
│  │ financial_data         SECRET                SECRET      ✓ YES      │   │
│  │ executive_comments     TOP_SECRET            SECRET      ✗ NO       │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 4: RETURN FILTERED RESPONSE                                     │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ {                                                                   │   │
│  │   "id": 12345,                                                      │   │
│  │   "name": "Q3 Strategic Plan",                                      │   │
│  │   "date": "2024-10-15",                                             │   │
│  │   "data": "Quarterly strategic initiatives...",                     │   │
│  │   "confidentialNotes": "Merger discussions...",                     │   │
│  │   "financialData": {"budget": 5000000},                             │   │
│  │   "visibleColumns": [                     ◄── Tells user what they  │   │
│  │     "id", "name", "date", "data",             can see               │   │
│  │     "confidentialNotes", "financialData"                            │   │
│  │   ],                                                                │   │
│  │   "hiddenColumns": [                      ◄── Tells user what was   │   │
│  │     "executiveComments"                       filtered out          │   │
│  │   ]                                                                 │   │
│  │ }                                                                   │   │
│  │                                                                     │   │
│  │ Note: executive_comments field is NOT in response                   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### READ Prevention Examples

| User | Clearance | Org Level | Dept | Requested Data | Result | Reason |
|------|-----------|-----------|------|----------------|--------|--------|
| dev.one | INTERNAL | INDIVIDUAL | ENGINEERING | CONFIDENTIAL, DEPARTMENT, ENGINEERING | **DENIED** | Org level too low (INDIVIDUAL < DEPARTMENT) |
| dev.one | INTERNAL | INDIVIDUAL | ENGINEERING | INTERNAL, INDIVIDUAL, owner=dev.one | **ALLOWED** | Own data |
| dev.one | INTERNAL | INDIVIDUAL | ENGINEERING | INTERNAL, INDIVIDUAL, owner=dev.two | **DENIED** | Not own data |
| alice.backend | CONFIDENTIAL | TEAM | ENGINEERING | CONFIDENTIAL, TEAM, SALES | **DENIED** | Wrong department |
| sarah.engineering | SECRET | DEPARTMENT | ENGINEERING | RESTRICTED, DEPARTMENT, ENGINEERING | **DENIED** | Clearance too low |
| john.ceo | TOP_SECRET | EXECUTIVE | - | Any data | **ALLOWED** | Full access |

### Listing Operations (GET /api/v1/data)

When listing multiple records, the system **pre-filters** results:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LIST OPERATION FILTERING                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  User: alice.backend (TEAM_LEAD, ENGINEERING/BACKEND, CONFIDENTIAL)         │
│                                                                              │
│  Database has 100 records:                                                  │
│  ├── 10 EXECUTIVE level records                    ──► NOT RETURNED         │
│  ├── 25 DEPARTMENT level records                                            │
│  │    ├── 15 ENGINEERING department               ──► NOT RETURNED          │
│  │    └── 10 SALES department                     ──► NOT RETURNED          │
│  ├── 40 TEAM level records                                                  │
│  │    ├── 12 ENGINEERING/BACKEND team             ──► RETURNED (filtered)   │
│  │    ├── 8 ENGINEERING/FRONTEND team             ──► NOT RETURNED          │
│  │    └── 20 other teams                          ──► NOT RETURNED          │
│  └── 25 INDIVIDUAL level records                                            │
│       ├── 5 alice.backend's own records           ──► RETURNED (filtered)   │
│       ├── 8 BACKEND team members' records         ──► RETURNED (filtered)   │
│       └── 12 other individuals' records           ──► NOT RETURNED          │
│                                                                              │
│  Alice sees: 12 + 5 + 8 = 25 records (with column filtering applied)        │
│                                                                              │
│  Records returned are filtered based on:                                    │
│  • Organization level hierarchy                                             │
│  • Department/Team membership                                               │
│  • Clearance vs sensitivity                                                 │
│  • Column-level filtering on each record                                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## UPDATE Operations

### What Happens When a User Updates Data

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         UPDATE OPERATION FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  PUT /api/v1/data/12345                                                     │
│  Authorization: Bearer <jwt_token>                                          │
│                                                                              │
│  Request Body:                                                              │
│  {                                                                          │
│    "name": "Q3 Strategic Plan - Updated",                                   │
│    "data": "Updated content...",                                            │
│    "confidentialNotes": "New merger details...",                            │
│    "financialData": {"budget": 6000000},          ◄── Attempting to update  │
│    "executiveComments": "CEO thoughts..."         ◄── Attempting to update  │
│  }                                                                          │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 1: VERIFY BASE ACCESS (Same as READ)                            │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ • Check organization level hierarchy                                │   │
│  │ • Check department/team membership                                  │   │
│  │ • Check clearance vs sensitivity                                    │   │
│  │ • Check context (IP, time)                                          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 2: DETERMINE WHICH FIELDS USER CAN UPDATE                       │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ User: sarah.engineering (SECRET clearance)                          │   │
│  │                                                                     │   │
│  │ Field                  Required Clearance    Can Update?            │   │
│  │ ─────────────────────  ──────────────────    ────────────           │   │
│  │ name                   PUBLIC                ✓ YES                  │   │
│  │ data                   INTERNAL              ✓ YES                  │   │
│  │ confidentialNotes      CONFIDENTIAL          ✓ YES                  │   │
│  │ financialData          SECRET                ✓ YES                  │   │
│  │ executiveComments      TOP_SECRET            ✗ NO (IGNORED)         │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 3: APPLY ONLY PERMITTED UPDATES                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ Fields Updated:                                                     │   │
│  │ • name: "Q3 Strategic Plan" → "Q3 Strategic Plan - Updated"         │   │
│  │ • data: [old content] → "Updated content..."                        │   │
│  │ • confidentialNotes: [old] → "New merger details..."                │   │
│  │ • financialData: {"budget": 5000000} → {"budget": 6000000}          │   │
│  │                                                                     │   │
│  │ Fields IGNORED (insufficient clearance):                            │   │
│  │ • executiveComments: NOT UPDATED (requires TOP_SECRET)              │   │
│  │                                                                     │   │
│  │ Metadata Auto-Updated:                                              │   │
│  │ • updated_at = current timestamp                                    │   │
│  │ • updated_by = sarah.engineering                                    │   │
│  │ • version = version + 1                                             │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 4: AUDIT LOG - FIELD-LEVEL TRACKING                             │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ Audit Record Created:                                               │   │
│  │ {                                                                   │   │
│  │   "action": "UPDATE",                                               │   │
│  │   "entityId": "12345",                                              │   │
│  │   "userId": "dept-eng-001",                                         │   │
│  │   "changedFields": ["name", "data", "confidentialNotes",            │   │
│  │                     "financialData"],                               │   │
│  │   "ignoredFields": ["executiveComments"],                           │   │
│  │   "ignoredReason": "INSUFFICIENT_CLEARANCE",                        │   │
│  │   "oldValues": {                                                    │   │
│  │     "name": "Q3 Strategic Plan",                                    │   │
│  │     "financialData": {"budget": 5000000}                            │   │
│  │   },                                                                │   │
│  │   "newValues": {                                                    │   │
│  │     "name": "Q3 Strategic Plan - Updated",                          │   │
│  │     "financialData": {"budget": 6000000}                            │   │
│  │   }                                                                 │   │
│  │ }                                                                   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### UPDATE Prevention Rules

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      UPDATE PROTECTION MECHANISMS                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  RULE 1: CANNOT UPDATE DATA YOU CANNOT READ                                 │
│  ═══════════════════════════════════════════                                │
│  If READ access is denied, UPDATE is automatically denied.                  │
│  You cannot modify what you cannot see.                                     │
│                                                                              │
│  RULE 2: CANNOT ELEVATE SENSITIVITY                                         │
│  ═════════════════════════════════════                                      │
│  Users CANNOT change sensitivity_level to a level above their clearance.    │
│                                                                              │
│  Example:                                                                   │
│  • User with CONFIDENTIAL clearance                                         │
│  • Data currently at INTERNAL sensitivity                                   │
│  • User tries to change to RESTRICTED                                       │
│  • DENIED: Cannot create data you couldn't read                             │
│                                                                              │
│  RULE 3: CANNOT ELEVATE ORGANIZATION LEVEL                                  │
│  ═════════════════════════════════════════════                              │
│  Users CANNOT change organization_level above their own level.              │
│                                                                              │
│  Example:                                                                   │
│  • User at TEAM level                                                       │
│  • Data currently at INDIVIDUAL level (user can access)                     │
│  • User tries to change to DEPARTMENT level                                 │
│  • DENIED: Cannot elevate above your own level                              │
│                                                                              │
│  RULE 4: CANNOT CHANGE OWNERSHIP METADATA                                   │
│  ═════════════════════════════════════════                                  │
│  Users CANNOT modify:                                                       │
│  • owner_id                                                                 │
│  • owner_department                                                         │
│  • owner_team                                                               │
│  • created_at                                                               │
│  • created_by                                                               │
│                                                                              │
│  These fields are IMMUTABLE after creation (except by ADMIN).               │
│                                                                              │
│  RULE 5: FIELD-LEVEL CLEARANCE ENFORCEMENT                                  │
│  ═════════════════════════════════════════════                              │
│  Each field has a clearance requirement. User can only update fields        │
│  where their clearance >= field requirement.                                │
│                                                                              │
│  Unauthorized field updates are SILENTLY IGNORED (not error).               │
│  This prevents information leakage about field existence.                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## DELETE Operations

### What Happens When a User Deletes Data

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DELETE OPERATION FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  DELETE /api/v1/data/12345                                                  │
│  Authorization: Bearer <jwt_token>                                          │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 1: VERIFY DELETE PERMISSION                                     │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ DELETE requires HIGHER privileges than READ:                        │   │
│  │                                                                     │   │
│  │ • Must pass all READ checks (org level, department, clearance)      │   │
│  │ • PLUS: Must be owner OR have admin/delete permission               │   │
│  │ • PLUS: Must have "data:delete" client role                         │   │
│  │                                                                     │   │
│  │ Additional Restrictions:                                            │   │
│  │ • INDIVIDUAL users can only delete their OWN data                   │   │
│  │ • TEAM leads can delete team data                                   │   │
│  │ • DEPARTMENT heads can delete department data                       │   │
│  │ • Only ADMIN can hard-delete (permanent)                            │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 2: SOFT DELETE (Default Behavior)                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ Data is NOT permanently removed. Instead:                           │   │
│  │                                                                     │   │
│  │ UPDATE my_data SET                                                  │   │
│  │   deleted = TRUE,                                                   │   │
│  │   deleted_at = CURRENT_TIMESTAMP,                                   │   │
│  │   deleted_by = 'user_id'                                            │   │
│  │ WHERE id = 12345;                                                   │   │
│  │                                                                     │   │
│  │ Record remains in database but:                                     │   │
│  │ • Excluded from all normal queries                                  │   │
│  │ • Visible only to ADMIN for recovery                                │   │
│  │ • Retained for audit/compliance purposes                            │   │
│  │ • Can be restored by ADMIN if needed                                │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                               │
│                              ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STEP 3: COMPREHENSIVE AUDIT                                          │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │ Delete operations have ENHANCED audit logging:                      │   │
│  │                                                                     │   │
│  │ {                                                                   │   │
│  │   "action": "DELETE",                                               │   │
│  │   "entityId": "12345",                                              │   │
│  │   "userId": "dept-eng-001",                                         │   │
│  │   "deleteType": "SOFT",                                             │   │
│  │   "deletedRecord": {                        ◄── Full record snapshot│   │
│  │     "name": "Q3 Strategic Plan",                                    │   │
│  │     "sensitivity_level": "CONFIDENTIAL",                            │   │
│  │     "organization_level": "DEPARTMENT",                             │   │
│  │     ...all fields preserved...                                      │   │
│  │   },                                                                │   │
│  │   "dataHash": "sha256:a7f3d2e1...",        ◄── Integrity hash      │   │
│  │   "canRecover": true,                                               │   │
│  │   "retentionUntil": "2031-01-15T00:00:00Z" ◄── 7 year retention    │   │
│  │ }                                                                   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### DELETE Prevention Rules

| User Level | Can Delete Own Data | Can Delete Team Data | Can Delete Dept Data | Can Hard Delete |
|------------|---------------------|----------------------|----------------------|-----------------|
| INDIVIDUAL | ✓ Soft only | ✗ | ✗ | ✗ |
| TEAM_LEAD | ✓ Soft only | ✓ Soft only | ✗ | ✗ |
| DEPT_HEAD | ✓ Soft only | ✓ Soft only | ✓ Soft only | ✗ |
| EXECUTIVE | ✓ Soft only | ✓ Soft only | ✓ Soft only | ✗ |
| ADMIN | ✓ Both | ✓ Both | ✓ Both | ✓ With approval |

---

## API Request/Response Examples

### Example 1: Successful CREATE by Team Lead

```http
POST /api/v1/data HTTP/1.1
Host: api.enterprise.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Backend Sprint Planning",
  "date": "2024-01-20",
  "data": "Sprint goals and tasks for Q1...",
  "sensitivityLevel": "CONFIDENTIAL",
  "organizationLevel": "TEAM"
}
```

**Response (201 Created):**
```json
{
  "id": 12346,
  "name": "Backend Sprint Planning",
  "date": "2024-01-20",
  "data": "Sprint goals and tasks for Q1...",
  "sensitivityLevel": "CONFIDENTIAL",
  "organizationLevel": "TEAM",
  "ownerId": "team-backend-001",
  "ownerDepartment": "ENGINEERING",
  "ownerTeam": "BACKEND",
  "createdAt": "2024-01-20T10:30:00Z",
  "createdBy": "team-backend-001",
  "version": 1,
  "_links": {
    "self": "/api/v1/data/12346",
    "update": "/api/v1/data/12346",
    "delete": "/api/v1/data/12346"
  }
}
```

### Example 2: DENIED CREATE - Clearance Too Low

```http
POST /api/v1/data HTTP/1.1
Host: api.enterprise.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Restricted Executive Report",
  "date": "2024-01-20",
  "data": "Should not be allowed...",
  "sensitivityLevel": "RESTRICTED",
  "organizationLevel": "INDIVIDUAL"
}
```

**Response (403 Forbidden):**
```json
{
  "error": "ACCESS_DENIED",
  "code": "DENIED_ATTRIBUTE",
  "message": "Insufficient clearance level to create RESTRICTED data",
  "details": {
    "requiredClearance": "TOP_SECRET",
    "userClearance": "CONFIDENTIAL",
    "operation": "CREATE",
    "requestedSensitivity": "RESTRICTED"
  },
  "timestamp": "2024-01-20T10:35:00Z",
  "correlationId": "corr_abc123def456",
  "supportReference": "Please contact security@enterprise.com if you believe this is in error"
}
```

### Example 3: READ with Column Filtering

```http
GET /api/v1/data/12345 HTTP/1.1
Host: api.enterprise.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response for INTERNAL clearance user (200 OK):**
```json
{
  "id": 12345,
  "name": "Q3 Strategic Plan",
  "date": "2024-10-15",
  "data": "Quarterly strategic initiatives...",
  "sensitivityLevel": "CONFIDENTIAL",
  "organizationLevel": "DEPARTMENT",
  "ownerId": "dept-eng-001",
  "ownerDepartment": "ENGINEERING",
  "createdAt": "2024-10-01T09:00:00Z",
  
  "_accessInfo": {
    "visibleColumns": ["id", "name", "date", "data", "sensitivityLevel", 
                       "organizationLevel", "ownerId", "ownerDepartment", "createdAt"],
    "hiddenColumns": ["confidentialNotes", "financialData", "executiveComments"],
    "hiddenReason": "INSUFFICIENT_CLEARANCE",
    "userClearance": "INTERNAL",
    "requiredForHidden": {
      "confidentialNotes": "CONFIDENTIAL",
      "financialData": "SECRET",
      "executiveComments": "TOP_SECRET"
    }
  }
}
```

**Response for SECRET clearance user (200 OK):**
```json
{
  "id": 12345,
  "name": "Q3 Strategic Plan",
  "date": "2024-10-15",
  "data": "Quarterly strategic initiatives...",
  "sensitivityLevel": "CONFIDENTIAL",
  "organizationLevel": "DEPARTMENT",
  "ownerId": "dept-eng-001",
  "ownerDepartment": "ENGINEERING",
  "createdAt": "2024-10-01T09:00:00Z",
  "confidentialNotes": "Merger discussions with Acme Corp...",
  "financialData": {
    "budget": 5000000,
    "spent": 3200000,
    "projected": 4800000
  },
  
  "_accessInfo": {
    "visibleColumns": ["id", "name", "date", "data", "sensitivityLevel", 
                       "organizationLevel", "ownerId", "ownerDepartment", 
                       "createdAt", "confidentialNotes", "financialData"],
    "hiddenColumns": ["executiveComments"],
    "hiddenReason": "INSUFFICIENT_CLEARANCE",
    "userClearance": "SECRET",
    "requiredForHidden": {
      "executiveComments": "TOP_SECRET"
    }
  }
}
```

### Example 4: DENIED READ - Wrong Department

```http
GET /api/v1/data/12345 HTTP/1.1
Host: api.enterprise.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**User:** sales.one (INDIVIDUAL, SALES department)  
**Data:** Engineering department data

**Response (403 Forbidden):**
```json
{
  "error": "ACCESS_DENIED",
  "code": "DENIED_ROLE",
  "message": "Access denied to data outside your organizational scope",
  "details": {
    "userDepartment": "SALES",
    "dataDepartment": "ENGINEERING",
    "userOrgLevel": "INDIVIDUAL",
    "dataOrgLevel": "DEPARTMENT",
    "operation": "READ"
  },
  "timestamp": "2024-01-20T10:40:00Z",
  "correlationId": "corr_xyz789abc012"
}
```

### Example 5: Partial UPDATE Success

```http
PUT /api/v1/data/12345 HTTP/1.1
Host: api.enterprise.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Q3 Strategic Plan - REVISED",
  "data": "Updated content...",
  "financialData": {"budget": 6000000},
  "executiveComments": "Trying to add executive comments..."
}
```

**User:** sarah.engineering (SECRET clearance)

**Response (200 OK - Partial Success):**
```json
{
  "id": 12345,
  "name": "Q3 Strategic Plan - REVISED",
  "date": "2024-10-15",
  "data": "Updated content...",
  "financialData": {"budget": 6000000},
  "updatedAt": "2024-01-20T10:45:00Z",
  "updatedBy": "dept-eng-001",
  "version": 4,
  
  "_updateInfo": {
    "fieldsUpdated": ["name", "data", "financialData"],
    "fieldsIgnored": ["executiveComments"],
    "ignoredReason": {
      "executiveComments": "INSUFFICIENT_CLEARANCE - requires TOP_SECRET"
    },
    "warning": "Some fields were not updated due to insufficient clearance"
  }
}
```

---

## Error Handling & Denial Responses

### Standard Error Response Format

```json
{
  "error": "ERROR_TYPE",
  "code": "SPECIFIC_CODE",
  "message": "Human-readable description",
  "details": {
    // Context-specific details
  },
  "timestamp": "ISO-8601 timestamp",
  "correlationId": "Request tracking ID",
  "path": "/api/v1/data/12345",
  "method": "GET|POST|PUT|DELETE",
  "supportReference": "Contact information for help"
}
```

### Error Codes by Denial Type

| Code | HTTP Status | Meaning | User Action |
|------|-------------|---------|-------------|
| `DENIED_ROLE` | 403 | Organization level or department mismatch | Request access from data owner |
| `DENIED_ATTRIBUTE` | 403 | Clearance level insufficient | Request clearance upgrade |
| `DENIED_CONTEXT` | 403 | Wrong network or outside business hours | Use approved network/time |
| `DENIED_ROW_LEVEL` | 403 | Specific record has blocking rule | Request exception from admin |
| `DENIED_COLUMN_LEVEL` | 200* | Field hidden due to clearance | Request clearance for field |
| `NOT_FOUND` | 404 | Record doesn't exist OR no access | Verify record ID |
| `UNAUTHORIZED` | 401 | Invalid or expired token | Re-authenticate |
| `FORBIDDEN` | 403 | Operation not permitted | Check permissions |

*Column-level denial returns 200 with filtered data, not an error

### Security: No Information Leakage

The system is designed to prevent information leakage:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    INFORMATION LEAKAGE PREVENTION                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  SCENARIO: User requests data they shouldn't know exists                    │
│                                                                              │
│  WRONG (Leaks information):                                                 │
│  GET /api/v1/data/99999                                                     │
│  Response: {"error": "ACCESS_DENIED", "message": "You cannot access         │
│             record 99999 which belongs to EXECUTIVE department"}            │
│                                                                              │
│  This reveals:                                                              │
│  • Record 99999 EXISTS                                                      │
│  • It belongs to EXECUTIVE department                                       │
│  • The user's access was specifically denied                                │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  CORRECT (No leakage):                                                      │
│  GET /api/v1/data/99999                                                     │
│  Response: {"error": "NOT_FOUND", "message": "Resource not found"}          │
│                                                                              │
│  Attacker cannot determine if:                                              │
│  • Record doesn't exist, OR                                                 │
│  • Record exists but user has no access                                     │
│                                                                              │
│  Both cases return identical 404 response.                                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Audit Trail for CRUD Operations

### What Gets Logged for Each Operation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    AUDIT LOG STRUCTURE BY OPERATION                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  CREATE OPERATION AUDIT                                                     │
│  ════════════════════                                                       │
│  {                                                                          │
│    "action": "CREATE",                                                      │
│    "entityType": "MyData",                                                  │
│    "entityId": "12346",                                                     │
│    "accessDecision": "GRANTED",                                             │
│    "newValues": { ...complete new record... },                              │
│    "validationsPassed": [                                                   │
│      "ORG_LEVEL_CHECK",                                                     │
│      "CLEARANCE_CHECK",                                                     │
│      "CONTEXT_CHECK"                                                        │
│    ],                                                                       │
│    "metadataAutoSet": {                                                     │
│      "ownerId": "from JWT",                                                 │
│      "ownerDepartment": "from JWT",                                         │
│      "ownerTeam": "from JWT"                                                │
│    }                                                                        │
│  }                                                                          │
│                                                                              │
│  READ OPERATION AUDIT                                                       │
│  ═══════════════════                                                        │
│  {                                                                          │
│    "action": "READ",                                                        │
│    "entityType": "MyData",                                                  │
│    "entityId": "12345",                                                     │
│    "accessDecision": "GRANTED",                                             │
│    "columnsVisible": ["id", "name", "date", "data"],                        │
│    "columnsHidden": ["financialData", "executiveComments"],                 │
│    "dataHash": "sha256:a7f3d2e1...",                                        │
│    "accessPath": {                                                          │
│      "rbacResult": "PASSED",                                                │
│      "abacResult": "PASSED",                                                │
│      "cbacResult": "PASSED",                                                │
│      "rowLevelResult": "PASSED"                                             │
│    }                                                                        │
│  }                                                                          │
│                                                                              │
│  UPDATE OPERATION AUDIT                                                     │
│  ══════════════════════                                                     │
│  {                                                                          │
│    "action": "UPDATE",                                                      │
│    "entityType": "MyData",                                                  │
│    "entityId": "12345",                                                     │
│    "accessDecision": "GRANTED",                                             │
│    "changedFields": ["name", "data", "financialData"],                      │
│    "ignoredFields": ["executiveComments"],                                  │
│    "ignoredReason": "INSUFFICIENT_CLEARANCE",                               │
│    "oldValues": {                                                           │
│      "name": "Old Name",                                                    │
│      "data": "Old content",                                                 │
│      "financialData": {"budget": 5000000}                                   │
│    },                                                                       │
│    "newValues": {                                                           │
│      "name": "New Name",                                                    │
│      "data": "New content",                                                 │
│      "financialData": {"budget": 6000000}                                   │
│    },                                                                       │
│    "versionChange": "3 → 4"                                                 │
│  }                                                                          │
│                                                                              │
│  DELETE OPERATION AUDIT                                                     │
│  ══════════════════════                                                     │
│  {                                                                          │
│    "action": "DELETE",                                                      │
│    "entityType": "MyData",                                                  │
│    "entityId": "12345",                                                     │
│    "accessDecision": "GRANTED",                                             │
│    "deleteType": "SOFT",                                                    │
│    "deletedRecord": { ...complete record snapshot... },                     │
│    "dataHash": "sha256:a7f3d2e1...",                                        │
│    "canRecover": true,                                                      │
│    "retentionPolicy": "7_YEARS",                                            │
│    "permanentDeletionDate": "2031-01-20"                                    │
│  }                                                                          │
│                                                                              │
│  DENIED OPERATION AUDIT                                                     │
│  ══════════════════════                                                     │
│  {                                                                          │
│    "action": "READ",                                                        │
│    "entityType": "MyData",                                                  │
│    "entityId": "12345",                                                     │
│    "accessDecision": "DENIED_ATTRIBUTE",                                    │
│    "denialReason": "User clearance INTERNAL < required CONFIDENTIAL",       │
│    "userContext": {                                                         │
│      "userId": "ind-dev-001",                                               │
│      "clearance": "INTERNAL",                                               │
│      "orgLevel": "INDIVIDUAL",                                              │
│      "department": "ENGINEERING"                                            │
│    },                                                                       │
│    "dataContext": {                                                         │
│      "sensitivity": "CONFIDENTIAL",                                         │
│      "orgLevel": "DEPARTMENT",                                              │
│      "ownerDepartment": "ENGINEERING"                                       │
│    },                                                                       │
│    "securityAlert": false  // true if pattern suggests attack               │
│  }                                                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Audit Query Examples

```sql
-- Find all denied CREATE attempts in last 24 hours
SELECT * FROM audit_log 
WHERE action = 'CREATE' 
  AND access_decision != 'GRANTED'
  AND timestamp > NOW() - INTERVAL '24 hours'
ORDER BY timestamp DESC;

-- Track all changes to a specific record
SELECT * FROM audit_log 
WHERE entity_id = '12345' 
ORDER BY timestamp ASC;

-- Find users who had fields filtered (column-level security in action)
SELECT user_id, COUNT(*) as filtered_reads
FROM audit_log 
WHERE action = 'READ' 
  AND column_level_filter IS NOT NULL
  AND timestamp > NOW() - INTERVAL '7 days'
GROUP BY user_id
ORDER BY filtered_reads DESC;

-- Detect potential data exfiltration (bulk reads)
SELECT user_id, COUNT(*) as read_count, 
       COUNT(DISTINCT entity_id) as unique_records
FROM audit_log 
WHERE action = 'READ' 
  AND timestamp > NOW() - INTERVAL '1 hour'
GROUP BY user_id
HAVING COUNT(*) > 100
ORDER BY read_count DESC;
```

---

## Summary: How Protection Works at Each Level

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PROTECTION SUMMARY BY USER LEVEL                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  EXECUTIVE (john.ceo)                                                       │
│  ═══════════════════                                                        │
│  Clearance: TOP_SECRET | Org Level: EXECUTIVE                               │
│                                                                              │
│  CREATE: Can create at any level, any sensitivity                           │
│  READ:   Can read all data, all fields visible                              │
│  UPDATE: Can update all data, all fields                                    │
│  DELETE: Can soft-delete any data                                           │
│                                                                              │
│  Restrictions: Cannot hard-delete without ADMIN approval                    │
│                Network: Should use Executive Network for RESTRICTED data    │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  DEPARTMENT HEAD (sarah.engineering)                                        │
│  ═══════════════════════════════════                                        │
│  Clearance: SECRET | Org Level: DEPARTMENT | Dept: ENGINEERING              │
│                                                                              │
│  CREATE: DEPARTMENT level or below, CONFIDENTIAL sensitivity or below       │
│  READ:   Engineering dept data only, SECRET fields and below visible        │
│  UPDATE: Engineering dept data only, SECRET fields and below                │
│  DELETE: Engineering dept data only (soft delete)                           │
│                                                                              │
│  Restrictions: Cannot see RESTRICTED data                                   │
│                Cannot see other departments' data                           │
│                Cannot see executive_comments field                          │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  TEAM LEAD (alice.backend)                                                  │
│  ═════════════════════════                                                  │
│  Clearance: CONFIDENTIAL | Org Level: TEAM | Dept: ENG | Team: BACKEND      │
│                                                                              │
│  CREATE: TEAM level or below, CONFIDENTIAL sensitivity or below             │
│  READ:   Backend team data only, CONFIDENTIAL fields and below visible      │
│  UPDATE: Backend team data only, CONFIDENTIAL fields and below              │
│  DELETE: Backend team data only (soft delete)                               │
│                                                                              │
│  Restrictions: Cannot see department-level data                             │
│                Cannot see other teams' data                                 │
│                Cannot see financial_data or executive_comments              │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  INDIVIDUAL (dev.one)                                                       │
│  ════════════════════                                                       │
│  Clearance: INTERNAL | Org Level: INDIVIDUAL | Dept: ENG | Team: BACKEND    │
│                                                                              │
│  CREATE: INDIVIDUAL level only, INTERNAL sensitivity or below               │
│  READ:   Own data only, INTERNAL fields and below visible                   │
│  UPDATE: Own data only, INTERNAL fields and below                           │
│  DELETE: Own data only (soft delete)                                        │
│                                                                              │
│  Restrictions: Cannot see anyone else's data                                │
│                Cannot see team/department/executive data                    │
│                Cannot see confidential_notes, financial_data, or            │
│                         executive_comments                                  │
│                Cannot create data others would need to see                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```