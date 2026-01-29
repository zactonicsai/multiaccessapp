package com.enterprise.datasharing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * Data Access Control entity that defines fine-grained access rules.
 * Controls who can access specific rows and columns in the data.
 * Supports RBAC, ABAC, and CBAC rules.
 */
@Entity
@Table(name = "data_access_control", indexes = {
    @Index(name = "idx_dac_data_id", columnList = "data_id"),
    @Index(name = "idx_dac_principal", columnList = "principal_type, principal_value"),
    @Index(name = "idx_dac_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataAccessControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Rule metadata
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // What data this rule applies to (null for global rules)
    @Column(name = "data_id")
    private Long dataId;

    // Who this rule applies to
    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, columnDefinition = "VARCHAR(50)")
    private PrincipalType principalType;

    @Column(name = "principal_value", nullable = false)
    private String principalValue;  // user ID, role name, department, team, clearance level

    // Access permissions
    @Column(name = "can_read")
    @Builder.Default
    private Boolean canRead = false;

    @Column(name = "can_create")
    @Builder.Default
    private Boolean canCreate = false;

    @Column(name = "can_update")
    @Builder.Default
    private Boolean canUpdate = false;

    @Column(name = "can_delete")
    @Builder.Default
    private Boolean canDelete = false;

    // Column-level access control (comma-separated column names)
    @Column(name = "visible_columns", columnDefinition = "TEXT")
    private String visibleColumns;  // Stored as comma-separated: "id,name,date,data"

    // ABAC - Attribute conditions (JSON string)
    @Column(name = "attribute_conditions", columnDefinition = "TEXT")
    private String attributeConditions;
    // Example: {"min_clearance": "CONFIDENTIAL", "department": "ENGINEERING"}

    // CBAC - Context conditions (JSON string)
    @Column(name = "context_conditions", columnDefinition = "TEXT")
    private String contextConditions;
    // Example: {"business_hours": true, "allowed_ips": ["10.0.0.0/8"]}

    // Time-based restrictions
    @Column(name = "valid_from")
    private OffsetDateTime validFrom;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;  // Higher = higher priority

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

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
