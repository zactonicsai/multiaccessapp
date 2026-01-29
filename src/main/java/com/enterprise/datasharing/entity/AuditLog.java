package com.enterprise.datasharing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Comprehensive audit log entity that tracks all data access and modifications.
 * Captures who, what, when, where, and how for security compliance.
 */
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Who
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "user_roles", length = 500)
    private String userRoles;

    @Column(name = "user_department", length = 100)
    private String userDepartment;

    @Column(name = "user_team", length = 100)
    private String userTeam;

    // What
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    // When
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Where (Context)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_uri", length = 1000)
    private String requestUri;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    // How (Access Control Decision)
    @Enumerated(EnumType.STRING)
    @Column(name = "access_decision", length = 50)
    private AccessDecision accessDecision;

    @Column(name = "access_reason", columnDefinition = "TEXT")
    private String accessReason;

    // RBAC info
    @Column(name = "required_role", length = 100)
    private String requiredRole;

    // ABAC info
    @Column(name = "attribute_conditions", columnDefinition = "TEXT")
    private String attributeConditions;

    // CBAC info
    @Column(name = "context_conditions", columnDefinition = "TEXT")
    private String contextConditions;

    // Correlation for request tracing
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "session_id")
    private String sessionId;

    // Result
    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Data hash for integrity verification
    @Column(name = "data_hash", length = 64)
    private String dataHash;

    /**
     * Audit actions that can be logged
     */
    public enum AuditAction {
        // CRUD Operations
        CREATE,
        READ,
        UPDATE,
        DELETE,
        
        // Bulk Operations
        BULK_READ,
        BULK_UPDATE,
        BULK_DELETE,
        EXPORT,
        
        // Access Control
        ACCESS_GRANTED,
        ACCESS_DENIED,
        ACCESS_ESCALATION,
        
        // Authentication/Authorization
        LOGIN,
        LOGOUT,
        TOKEN_REFRESH,
        PERMISSION_CHECK,
        
        // Administrative
        CONFIG_CHANGE,
        ROLE_ASSIGNMENT,
        ACCESS_RULE_CHANGE
    }

    /**
     * Access decision results
     */
    public enum AccessDecision {
        GRANTED,
        DENIED_ROLE,           // Denied due to insufficient role
        DENIED_ATTRIBUTE,      // Denied due to attribute mismatch
        DENIED_CONTEXT,        // Denied due to context violation
        DENIED_ROW_LEVEL,      // Denied due to row-level restriction
        DENIED_COLUMN_LEVEL,   // Denied due to column-level restriction
        PARTIAL                // Partial access (some columns filtered)
    }
}
