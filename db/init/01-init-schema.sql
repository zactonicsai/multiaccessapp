-- =====================================================
-- Enterprise Data Sharing - Database Initialization
-- =====================================================

-- Create schema for Keycloak (separate from application)
CREATE SCHEMA IF NOT EXISTS keycloak;

-- =====================================================
-- ENUM TYPES
-- =====================================================

CREATE TYPE sensitivity_level AS ENUM (
    'PUBLIC',
    'INTERNAL',
    'CONFIDENTIAL',
    'RESTRICTED'
);

CREATE TYPE organization_level AS ENUM (
    'EXECUTIVE',
    'DEPARTMENT',
    'TEAM',
    'INDIVIDUAL'
);

CREATE TYPE clearance_level AS ENUM (
    'PUBLIC',
    'INTERNAL',
    'CONFIDENTIAL',
    'SECRET',
    'TOP_SECRET'
);

CREATE TYPE access_decision_type AS ENUM (
    'GRANTED',
    'DENIED_ROLE',
    'DENIED_ATTRIBUTE',
    'DENIED_CONTEXT',
    'DENIED_ROW_LEVEL',
    'DENIED_COLUMN_LEVEL'
);

CREATE TYPE principal_type AS ENUM (
    'USER',
    'ROLE',
    'DEPARTMENT',
    'TEAM',
    'ORGANIZATION',
    'CLEARANCE',
    'ALL'
);

-- =====================================================
-- MAIN DATA TABLE
-- =====================================================

CREATE TABLE my_data (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    data TEXT,
    sensitivity_level sensitivity_level NOT NULL DEFAULT 'INTERNAL',
    organization_level organization_level NOT NULL DEFAULT 'INDIVIDUAL',
    owner_id VARCHAR(255) NOT NULL,
    owner_department VARCHAR(100),
    owner_team VARCHAR(100),
    confidential_notes TEXT,
    financial_data JSONB,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_my_data_owner ON my_data(owner_id);
CREATE INDEX idx_my_data_department ON my_data(owner_department);
CREATE INDEX idx_my_data_team ON my_data(owner_team);
CREATE INDEX idx_my_data_org_level ON my_data(organization_level);
CREATE INDEX idx_my_data_sensitivity ON my_data(sensitivity_level);
CREATE INDEX idx_my_data_date ON my_data(date);
CREATE INDEX idx_my_data_deleted ON my_data(deleted);

-- =====================================================
-- USER ATTRIBUTES TABLE (ABAC)
-- =====================================================

CREATE TABLE user_attribute (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    department VARCHAR(100),
    team VARCHAR(100),
    clearance_level clearance_level NOT NULL DEFAULT 'PUBLIC',
    organization_level organization_level NOT NULL DEFAULT 'INDIVIDUAL',
    manager_id VARCHAR(255),
    is_manager BOOLEAN DEFAULT FALSE,
    is_department_head BOOLEAN DEFAULT FALSE,
    is_executive BOOLEAN DEFAULT FALSE,
    attributes JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_user_attr_user_id ON user_attribute(user_id);
CREATE INDEX idx_user_attr_department ON user_attribute(department);
CREATE INDEX idx_user_attr_team ON user_attribute(team);
CREATE INDEX idx_user_attr_clearance ON user_attribute(clearance_level);
CREATE INDEX idx_user_attr_manager ON user_attribute(manager_id);

-- =====================================================
-- DATA ACCESS CONTROL TABLE (Row/Column Level)
-- =====================================================

CREATE TABLE data_access_control (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(255) NOT NULL,
    description TEXT,
    data_id BIGINT REFERENCES my_data(id) ON DELETE CASCADE,
    principal_type principal_type NOT NULL,
    principal_value VARCHAR(255) NOT NULL,
    can_read BOOLEAN DEFAULT FALSE,
    can_create BOOLEAN DEFAULT FALSE,
    can_update BOOLEAN DEFAULT FALSE,
    can_delete BOOLEAN DEFAULT FALSE,
    visible_columns TEXT[],
    attribute_conditions JSONB,
    context_conditions JSONB,
    valid_from TIMESTAMP WITH TIME ZONE,
    valid_until TIMESTAMP WITH TIME ZONE,
    priority INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_dac_data_id ON data_access_control(data_id);
CREATE INDEX idx_dac_principal ON data_access_control(principal_type, principal_value);
CREATE INDEX idx_dac_active ON data_access_control(active);
CREATE INDEX idx_dac_validity ON data_access_control(valid_from, valid_until);

-- =====================================================
-- AUDIT LOG TABLE
-- =====================================================

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255),
    access_decision access_decision_type,
    denial_reason TEXT,
    rbac_roles TEXT[],
    abac_attributes JSONB,
    cbac_context JSONB,
    row_level_check BOOLEAN,
    column_level_filter TEXT[],
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_uri VARCHAR(1000),
    request_method VARCHAR(10),
    correlation_id VARCHAR(36),
    session_id VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    data_hash VARCHAR(64),
    additional_info JSONB
);

CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_access_decision ON audit_log(access_decision);
CREATE INDEX idx_audit_correlation ON audit_log(correlation_id);
CREATE INDEX idx_audit_ip ON audit_log(ip_address);

-- Partition audit log by month for performance (optional, comment out if not needed)
-- CREATE TABLE audit_log_template (LIKE audit_log INCLUDING ALL) PARTITION BY RANGE (timestamp);

-- =====================================================
-- FUNCTIONS AND TRIGGERS
-- =====================================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_my_data_updated_at
    BEFORE UPDATE ON my_data
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_attribute_updated_at
    BEFORE UPDATE ON user_attribute
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_data_access_control_updated_at
    BEFORE UPDATE ON data_access_control
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SAMPLE DATA - Users with different roles and clearances
-- =====================================================

-- Executive user
INSERT INTO user_attribute (user_id, username, email, department, team, clearance_level, organization_level, is_executive, is_manager, is_department_head)
VALUES ('exec-001', 'john.ceo', 'john.ceo@enterprise.com', 'EXECUTIVE', 'LEADERSHIP', 'TOP_SECRET', 'EXECUTIVE', TRUE, TRUE, TRUE);

-- Department heads
INSERT INTO user_attribute (user_id, username, email, department, team, clearance_level, organization_level, is_manager, is_department_head, manager_id)
VALUES 
('dept-eng-001', 'sarah.engineering', 'sarah.engineering@enterprise.com', 'ENGINEERING', 'MANAGEMENT', 'SECRET', 'DEPARTMENT', TRUE, TRUE, 'exec-001'),
('dept-sales-001', 'mike.sales', 'mike.sales@enterprise.com', 'SALES', 'MANAGEMENT', 'SECRET', 'DEPARTMENT', TRUE, TRUE, 'exec-001'),
('dept-hr-001', 'lisa.hr', 'lisa.hr@enterprise.com', 'HR', 'MANAGEMENT', 'SECRET', 'DEPARTMENT', TRUE, TRUE, 'exec-001');

-- Team leads
INSERT INTO user_attribute (user_id, username, email, department, team, clearance_level, organization_level, is_manager, manager_id)
VALUES 
('team-backend-001', 'alice.backend', 'alice.backend@enterprise.com', 'ENGINEERING', 'BACKEND', 'CONFIDENTIAL', 'TEAM', TRUE, 'dept-eng-001'),
('team-frontend-001', 'bob.frontend', 'bob.frontend@enterprise.com', 'ENGINEERING', 'FRONTEND', 'CONFIDENTIAL', 'TEAM', TRUE, 'dept-eng-001'),
('team-enterprise-001', 'carol.enterprise', 'carol.enterprise@enterprise.com', 'SALES', 'ENTERPRISE', 'CONFIDENTIAL', 'TEAM', TRUE, 'dept-sales-001');

-- Individual contributors
INSERT INTO user_attribute (user_id, username, email, department, team, clearance_level, organization_level, manager_id)
VALUES 
('ind-dev-001', 'dev.one', 'dev.one@enterprise.com', 'ENGINEERING', 'BACKEND', 'INTERNAL', 'INDIVIDUAL', 'team-backend-001'),
('ind-dev-002', 'dev.two', 'dev.two@enterprise.com', 'ENGINEERING', 'BACKEND', 'INTERNAL', 'INDIVIDUAL', 'team-backend-001'),
('ind-dev-003', 'dev.three', 'dev.three@enterprise.com', 'ENGINEERING', 'FRONTEND', 'INTERNAL', 'INDIVIDUAL', 'team-frontend-001'),
('ind-sales-001', 'sales.one', 'sales.one@enterprise.com', 'SALES', 'ENTERPRISE', 'INTERNAL', 'INDIVIDUAL', 'team-enterprise-001'),
('ind-hr-001', 'hr.specialist', 'hr.specialist@enterprise.com', 'HR', 'RECRUITING', 'CONFIDENTIAL', 'INDIVIDUAL', 'dept-hr-001');

-- =====================================================
-- SAMPLE DATA - My Data records at different levels
-- =====================================================

-- Executive level data
INSERT INTO my_data (name, date, data, sensitivity_level, organization_level, owner_id, owner_department, owner_team, confidential_notes, financial_data, created_by)
VALUES 
('Q4 Strategic Plan', '2024-01-15', 'Company-wide strategic initiatives for Q4', 'RESTRICTED', 'EXECUTIVE', 'exec-001', 'EXECUTIVE', 'LEADERSHIP', 
 'Confidential merger discussions', '{"revenue_target": 50000000, "budget": 10000000}', 'exec-001'),
('Annual Board Report', '2024-01-01', 'Annual performance report for board review', 'RESTRICTED', 'EXECUTIVE', 'exec-001', 'EXECUTIVE', 'LEADERSHIP',
 'Executive compensation details', '{"profit": 15000000, "growth": 0.25}', 'exec-001');

-- Department level data
INSERT INTO my_data (name, date, data, sensitivity_level, organization_level, owner_id, owner_department, owner_team, confidential_notes, financial_data, created_by)
VALUES 
('Engineering Roadmap 2024', '2024-01-10', 'Technical roadmap and priorities for Engineering department', 'CONFIDENTIAL', 'DEPARTMENT', 'dept-eng-001', 'ENGINEERING', 'MANAGEMENT',
 'Planned tech stack migration', '{"budget": 2000000}', 'dept-eng-001'),
('Sales Pipeline Q1', '2024-01-05', 'Q1 sales pipeline and forecast', 'CONFIDENTIAL', 'DEPARTMENT', 'dept-sales-001', 'SALES', 'MANAGEMENT',
 'Key account negotiations', '{"pipeline_value": 8000000, "expected_close": 5000000}', 'dept-sales-001'),
('HR Policy Updates', '2024-01-08', 'Updated HR policies for 2024', 'INTERNAL', 'DEPARTMENT', 'dept-hr-001', 'HR', 'MANAGEMENT',
 'Salary band adjustments', '{"training_budget": 500000}', 'dept-hr-001');

-- Team level data
INSERT INTO my_data (name, date, data, sensitivity_level, organization_level, owner_id, owner_department, owner_team, confidential_notes, created_by)
VALUES 
('Backend Architecture Review', '2024-01-12', 'Microservices architecture review and recommendations', 'INTERNAL', 'TEAM', 'team-backend-001', 'ENGINEERING', 'BACKEND',
 'Security vulnerability assessment', 'team-backend-001'),
('Frontend Performance Report', '2024-01-11', 'UI performance metrics and optimization plan', 'INTERNAL', 'TEAM', 'team-frontend-001', 'ENGINEERING', 'FRONTEND',
 NULL, 'team-frontend-001'),
('Enterprise Sales Strategy', '2024-01-09', 'Strategy for enterprise client acquisition', 'CONFIDENTIAL', 'TEAM', 'team-enterprise-001', 'SALES', 'ENTERPRISE',
 'Competitor pricing intel', 'team-enterprise-001');

-- Individual level data
INSERT INTO my_data (name, date, data, sensitivity_level, organization_level, owner_id, owner_department, owner_team, created_by)
VALUES 
('API Integration Notes', '2024-01-14', 'Personal notes on third-party API integrations', 'INTERNAL', 'INDIVIDUAL', 'ind-dev-001', 'ENGINEERING', 'BACKEND', 'ind-dev-001'),
('Code Review Checklist', '2024-01-13', 'Personal code review checklist and guidelines', 'PUBLIC', 'INDIVIDUAL', 'ind-dev-002', 'ENGINEERING', 'BACKEND', 'ind-dev-002'),
('React Component Library', '2024-01-12', 'Documentation for reusable React components', 'PUBLIC', 'INDIVIDUAL', 'ind-dev-003', 'ENGINEERING', 'FRONTEND', 'ind-dev-003'),
('Client Meeting Notes', '2024-01-11', 'Notes from Acme Corp meeting', 'CONFIDENTIAL', 'INDIVIDUAL', 'ind-sales-001', 'SALES', 'ENTERPRISE', 'ind-sales-001'),
('Candidate Evaluations', '2024-01-10', 'Interview evaluations for backend positions', 'CONFIDENTIAL', 'INDIVIDUAL', 'ind-hr-001', 'HR', 'RECRUITING', 'ind-hr-001');

-- =====================================================
-- SAMPLE DATA - Access Control Rules
-- =====================================================

-- Global rule: All authenticated users can read PUBLIC data
INSERT INTO data_access_control (rule_name, description, principal_type, principal_value, can_read, visible_columns, priority, created_by)
VALUES ('Public Data Access', 'All users can read public sensitivity data', 'ALL', '*', TRUE, 
        ARRAY['id', 'name', 'date', 'data', 'owner_id', 'created_at'], 10, 'system');

-- Department-based column visibility
INSERT INTO data_access_control (rule_name, description, principal_type, principal_value, can_read, can_update, visible_columns, priority, created_by)
VALUES 
('HR Financial Access', 'HR department can see financial data for salary reviews', 'DEPARTMENT', 'HR', TRUE, FALSE,
 ARRAY['id', 'name', 'date', 'data', 'financial_data', 'owner_id', 'created_at'], 20, 'system'),
('Engineering Full Access', 'Engineering can see technical details', 'DEPARTMENT', 'ENGINEERING', TRUE, TRUE,
 ARRAY['id', 'name', 'date', 'data', 'metadata', 'owner_id', 'owner_team', 'created_at', 'updated_at'], 20, 'system');

-- Clearance-based access
INSERT INTO data_access_control (rule_name, description, principal_type, principal_value, can_read, visible_columns, attribute_conditions, priority, created_by)
VALUES 
('Confidential Notes Access', 'Users with CONFIDENTIAL+ clearance can see confidential notes', 'CLEARANCE', 'CONFIDENTIAL', TRUE,
 ARRAY['id', 'name', 'date', 'data', 'confidential_notes', 'owner_id', 'created_at'],
 '{"min_clearance": "CONFIDENTIAL"}', 30, 'system'),
('Secret Financial Access', 'Users with SECRET+ clearance can see all financial data', 'CLEARANCE', 'SECRET', TRUE,
 ARRAY['id', 'name', 'date', 'data', 'confidential_notes', 'financial_data', 'owner_id', 'created_at'],
 '{"min_clearance": "SECRET"}', 40, 'system');

-- Time-based access rule
INSERT INTO data_access_control (rule_name, description, principal_type, principal_value, can_read, can_update, context_conditions, valid_from, valid_until, priority, created_by)
VALUES ('Business Hours Only', 'Sensitive data only accessible during business hours', 'ALL', '*', TRUE, TRUE,
        '{"business_hours": true, "allowed_ips": ["10.0.0.0/8", "192.168.0.0/16"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 year', 5, 'system');

-- Row-level access for specific data
INSERT INTO data_access_control (rule_name, description, data_id, principal_type, principal_value, can_read, can_update, can_delete, visible_columns, priority, created_by)
SELECT 
    'Strategic Plan Access',
    'Only executives can access strategic plan',
    id,
    'ORGANIZATION',
    'EXECUTIVE',
    TRUE, TRUE, FALSE,
    ARRAY['id', 'name', 'date', 'data', 'confidential_notes', 'financial_data', 'owner_id', 'created_at', 'updated_at'],
    50,
    'system'
FROM my_data WHERE name = 'Q4 Strategic Plan';

-- =====================================================
-- VIEWS FOR REPORTING
-- =====================================================

-- View for data access summary
CREATE OR REPLACE VIEW v_data_access_summary AS
SELECT 
    d.id,
    d.name,
    d.sensitivity_level,
    d.organization_level,
    d.owner_department,
    d.owner_team,
    COUNT(DISTINCT dac.id) as access_rules_count,
    ARRAY_AGG(DISTINCT dac.principal_type) as access_principal_types
FROM my_data d
LEFT JOIN data_access_control dac ON dac.data_id = d.id OR dac.data_id IS NULL
WHERE d.deleted = FALSE AND dac.active = TRUE
GROUP BY d.id, d.name, d.sensitivity_level, d.organization_level, d.owner_department, d.owner_team;

-- View for audit summary
CREATE OR REPLACE VIEW v_audit_summary AS
SELECT 
    DATE_TRUNC('day', timestamp) as audit_date,
    user_id,
    action,
    access_decision,
    COUNT(*) as event_count,
    COUNT(DISTINCT entity_id) as unique_entities
FROM audit_log
GROUP BY DATE_TRUNC('day', timestamp), user_id, action, access_decision
ORDER BY audit_date DESC, event_count DESC;

-- View for access violations
CREATE OR REPLACE VIEW v_access_violations AS
SELECT 
    timestamp,
    user_id,
    username,
    action,
    entity_type,
    entity_id,
    access_decision,
    denial_reason,
    ip_address
FROM audit_log
WHERE access_decision != 'GRANTED'
ORDER BY timestamp DESC;

-- =====================================================
-- GRANTS (adjust as needed for your setup)
-- =====================================================

-- Grant usage on sequences
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO datasharing;

-- Grant table permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO datasharing;

COMMIT;
