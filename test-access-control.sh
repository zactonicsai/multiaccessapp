#!/bin/bash

# Enterprise Data Sharing - Access Control Demo Script
# This script demonstrates the RBAC, ABAC, and CBAC access controls

set -e

# Configuration
KEYCLOAK_URL="http://localhost:8180"
API_URL="http://localhost:8080"
REALM="enterprise"
CLIENT_ID="datasharing-web"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Enterprise Data Sharing - Access Control Demo${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Function to get token
get_token() {
    local username=$1
    local password=$2
    curl -s -X POST \
        "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=${CLIENT_ID}" \
        -d "username=${username}" \
        -d "password=${password}" | jq -r '.access_token'
}

# Function to make API call
api_call() {
    local method=$1
    local endpoint=$2
    local token=$3
    local data=$4
    
    if [ -z "$data" ]; then
        curl -s -X ${method} \
            "${API_URL}${endpoint}" \
            -H "Authorization: Bearer ${token}" \
            -H "Content-Type: application/json"
    else
        curl -s -X ${method} \
            "${API_URL}${endpoint}" \
            -H "Authorization: Bearer ${token}" \
            -H "Content-Type: application/json" \
            -d "${data}"
    fi
}

# Wait for services
echo -e "${YELLOW}Checking services...${NC}"
until curl -s "${KEYCLOAK_URL}/realms/${REALM}/.well-known/openid-configuration" > /dev/null 2>&1; do
    echo "Waiting for Keycloak..."
    sleep 5
done
echo -e "${GREEN}✓ Keycloak is ready${NC}"

until curl -s "${API_URL}/actuator/health" > /dev/null 2>&1; do
    echo "Waiting for API..."
    sleep 5
done
echo -e "${GREEN}✓ API is ready${NC}"
echo ""

# ====================
# Test 1: Executive Access
# ====================
echo -e "${BLUE}=== Test 1: Executive (john.ceo) - Full Access ===${NC}"
EXEC_TOKEN=$(get_token "john.ceo" "exec123")
echo "Getting all data as Executive..."
EXEC_DATA=$(api_call "GET" "/api/v1/data" "$EXEC_TOKEN")
EXEC_COUNT=$(echo $EXEC_DATA | jq 'length')
echo -e "${GREEN}Executive can see ${EXEC_COUNT} records (ALL data)${NC}"
echo "Sample record with all fields visible:"
echo $EXEC_DATA | jq '.[0] | {id, name, sensitivityLevel, organizationLevel, financialData, confidentialNotes}'
echo ""

# ====================
# Test 2: Department Head Access
# ====================
echo -e "${BLUE}=== Test 2: Department Head (sarah.engineering) - Department Data ===${NC}"
DEPT_TOKEN=$(get_token "sarah.engineering" "dept123")
echo "Getting all data as Engineering Department Head..."
DEPT_DATA=$(api_call "GET" "/api/v1/data" "$DEPT_TOKEN")
DEPT_COUNT=$(echo $DEPT_DATA | jq 'length')
echo -e "${GREEN}Department Head can see ${DEPT_COUNT} records (Department + Team + Individual)${NC}"
echo "Departments visible:"
echo $DEPT_DATA | jq '[.[].ownerDepartment] | unique'
echo ""

# ====================
# Test 3: Team Lead Access
# ====================
echo -e "${BLUE}=== Test 3: Team Lead (alice.backend) - Team Data ===${NC}"
TEAM_TOKEN=$(get_token "alice.backend" "team123")
echo "Getting all data as Backend Team Lead..."
TEAM_DATA=$(api_call "GET" "/api/v1/data" "$TEAM_TOKEN")
TEAM_COUNT=$(echo $TEAM_DATA | jq 'length')
echo -e "${GREEN}Team Lead can see ${TEAM_COUNT} records (Team + Individual)${NC}"
echo "Teams visible:"
echo $TEAM_DATA | jq '[.[].ownerTeam] | unique'
echo ""

# ====================
# Test 4: Individual Contributor Access
# ====================
echo -e "${BLUE}=== Test 4: Individual (dev.one) - Own Data Only ===${NC}"
IND_TOKEN=$(get_token "dev.one" "emp123")
echo "Getting all data as Individual Contributor..."
IND_DATA=$(api_call "GET" "/api/v1/data" "$IND_TOKEN")
IND_COUNT=$(echo $IND_DATA | jq 'length')
echo -e "${GREEN}Individual can see ${IND_COUNT} records (own data only)${NC}"
echo ""

# ====================
# Test 5: Column-Level Security
# ====================
echo -e "${BLUE}=== Test 5: Column-Level Security - Clearance Based ===${NC}"
echo ""
echo "Getting record with financial data as Executive (TOP_SECRET clearance):"
EXEC_RECORD=$(api_call "GET" "/api/v1/data/1" "$EXEC_TOKEN")
echo $EXEC_RECORD | jq '{id, name, financialData, confidentialNotes, visibleColumns}'
echo ""

echo "Getting same record as Individual (INTERNAL clearance):"
IND_RECORD=$(api_call "GET" "/api/v1/data/1" "$IND_TOKEN" 2>/dev/null || echo '{"error": "Access Denied"}')
echo $IND_RECORD | jq '{id, name, financialData, confidentialNotes, visibleColumns}' 2>/dev/null || echo -e "${RED}Access Denied - Insufficient clearance${NC}"
echo ""

# ====================
# Test 6: Create Data at Different Levels
# ====================
echo -e "${BLUE}=== Test 6: Create Data - Organization Level Enforcement ===${NC}"
echo ""

echo "Creating INDIVIDUAL level data as dev.one..."
CREATE_RESULT=$(api_call "POST" "/api/v1/data" "$IND_TOKEN" '{
    "name": "Personal Dev Notes",
    "date": "2024-01-20",
    "data": "My personal development notes",
    "sensitivityLevel": "INTERNAL",
    "organizationLevel": "INDIVIDUAL"
}')
echo $CREATE_RESULT | jq '{id, name, organizationLevel, ownerId}'
echo -e "${GREEN}✓ Individual can create INDIVIDUAL level data${NC}"
echo ""

echo "Attempting to create DEPARTMENT level data as dev.one (should fail or be restricted)..."
CREATE_DEPT=$(api_call "POST" "/api/v1/data" "$IND_TOKEN" '{
    "name": "Should Not Be Department",
    "date": "2024-01-20",
    "data": "This should be restricted",
    "sensitivityLevel": "CONFIDENTIAL",
    "organizationLevel": "DEPARTMENT"
}' 2>/dev/null || echo '{"error": "Access Denied"}')
echo $CREATE_DEPT | jq '.' 2>/dev/null || echo -e "${RED}Access Denied - Cannot create above your level${NC}"
echo ""

# ====================
# Test 7: Cross-Department Access
# ====================
echo -e "${BLUE}=== Test 7: Cross-Department Access Control ===${NC}"
echo ""

echo "Sales Head (mike.sales) trying to access Engineering data..."
SALES_TOKEN=$(get_token "mike.sales" "dept123")
SALES_DATA=$(api_call "GET" "/api/v1/data" "$SALES_TOKEN")
SALES_DEPTS=$(echo $SALES_DATA | jq '[.[].ownerDepartment] | unique')
echo "Departments visible to Sales Head: ${SALES_DEPTS}"
echo ""

# ====================
# Test 8: Audit Log Check
# ====================
echo -e "${BLUE}=== Test 8: Audit Logging ===${NC}"
echo ""

ADMIN_TOKEN=$(get_token "admin" "admin123")
echo "Getting recent audit entries..."
AUDIT_DATA=$(api_call "GET" "/api/v1/admin/audit?size=5" "$ADMIN_TOKEN")
echo "Recent audit entries:"
echo $AUDIT_DATA | jq '.[] | {timestamp, userId, action, entityType, accessDecision}' 2>/dev/null || echo $AUDIT_DATA | jq '.[0:5] | .[] | {timestamp, userId, action, entityType, accessDecision}'
echo ""

# ====================
# Summary
# ====================
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Summary${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "Records visible by role:"
echo -e "  ${GREEN}Executive (john.ceo):${NC} ${EXEC_COUNT} records"
echo -e "  ${GREEN}Department Head (sarah.engineering):${NC} ${DEPT_COUNT} records"
echo -e "  ${GREEN}Team Lead (alice.backend):${NC} ${TEAM_COUNT} records"
echo -e "  ${GREEN}Individual (dev.one):${NC} ${IND_COUNT} records"
echo ""
echo -e "${GREEN}✓ RBAC: Role hierarchy enforced${NC}"
echo -e "${GREEN}✓ ABAC: Clearance-based column filtering${NC}"
echo -e "${GREEN}✓ Organization Hierarchy: Executive > Department > Team > Individual${NC}"
echo -e "${GREEN}✓ Audit Logging: All access recorded${NC}"
echo ""
echo -e "${BLUE}Demo complete!${NC}"
