package com.enterprise.datasharing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Access Control entity that defines fine-grained access rules.
 * Controls who can access specific rows and columns in the data.
 * Supports RBAC, ABAC, and CBAC rules.
 */
@Entity
@Table(name = "data_access_control", indexes = {
    @Index(name = "idx_dac_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_dac_principal", columnList = "principal_type, principal_id"),
    @Index(name = "idx_dac_active", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataAccessControl {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // What entity this rule applies to
    @Column(name = "entity_type", nullable = false)
    private String entityType;  // e.g., "MyData"

    @Column(name = "entity_id")
    private String entityId;  // Specific row ID, null for table-wide rules

    // Who this rule applies to
    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false)
    private PrincipalType principalType;

    @Column(name = "principal_id")
    private String principalId;  // user ID, role name, department ID, team ID

    // Access permissions
    @Column(name = "can_read", nullable = false)
    @Builder.Default
    private Boolean canRead = false;

    @Column(name = "can_create", nullable = false)
    @Builder.Default
    private Boolean canCreate = false;

    @Column(name = "can_update", nullable = false)
    @Builder.Default
    private Boolean canUpdate = false;

    @Column(name = "can_delete", nullable = false)
    @Builder.Default
    private Boolean canDelete = false;

    // Column-level access control (JSON array of column names)
    @Column(name = "allowed_columns", columnDefinition = "TEXT")
    private String allowedColumns;  // JSON: ["name", "dataDate", "data"]

    @Column(name = "denied_columns", columnDefinition = "TEXT")
    private String deniedColumns;   // JSON: ["confidentialNotes", "financialData"]

    // ABAC - Attribute conditions (JSON)
    @Column(name = "attribute_conditions", columnDefinition = "TEXT")
    private String attributeConditions;
    // Example: {"department": "ENGINEERING", "clearanceLevel": "SECRET"}

    // CBAC - Context conditions (JSON)
    @Column(name = "context_conditions", columnDefinition = "TEXT")
    private String contextConditions;
    // Example: {"requireBusinessHours": true, "allowedIpRanges": ["10.0.0.0/8"]}

    // Rule metadata
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "description")
    private String description;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;  // Lower = higher priority

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Time-based restrictions
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Version
    private Long version;

    /**
     * Types of principals that access rules can apply to
     */
    public enum PrincipalType {
        USER,           // Specific user
        ROLE,           // Role-based (RBAC)
        DEPARTMENT,     // Department-based (ABAC)
        TEAM,           // Team-based (ABAC)
        ORGANIZATION,   // Organization-wide
        CLEARANCE,      // Clearance level (ABAC)
        ALL             // Everyone (with conditions)
    }
}
