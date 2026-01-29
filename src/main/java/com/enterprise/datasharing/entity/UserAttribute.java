package com.enterprise.datasharing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * User Attribute entity for ABAC (Attribute-Based Access Control).
 * Stores additional user attributes beyond what Keycloak provides.
 * These attributes are used to make access control decisions.
 */
@Entity
@Table(name = "user_attribute", indexes = {
    @Index(name = "idx_user_attr_user_id", columnList = "user_id", unique = true),
    @Index(name = "idx_user_attr_department", columnList = "department"),
    @Index(name = "idx_user_attr_team", columnList = "team"),
    @Index(name = "idx_user_attr_clearance", columnList = "clearance_level")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email")
    private String email;

    // Organizational hierarchy
    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "team", length = 100)
    private String team;

    // Clearance levels (for ABAC)
    @Enumerated(EnumType.STRING)
    @Column(name = "clearance_level", nullable = false, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private ClearanceLevel clearanceLevel = ClearanceLevel.PUBLIC;

    // Organization level (for RBAC)
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_level", nullable = false, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private OrganizationLevel organizationLevel = OrganizationLevel.INDIVIDUAL;

    // Manager hierarchy
    @Column(name = "manager_id")
    private String managerId;

    @Column(name = "is_manager")
    @Builder.Default
    private Boolean isManager = false;

    @Column(name = "is_department_head")
    @Builder.Default
    private Boolean isDepartmentHead = false;

    @Column(name = "is_executive")
    @Builder.Default
    private Boolean isExecutive = false;

    // Additional custom attributes (JSON string)
    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Status
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Organization levels for hierarchy-based access
     */
    public enum OrganizationLevel {
        EXECUTIVE,      // C-level, VP, Director
        DEPARTMENT,     // Department/Division head
        TEAM,           // Team lead/supervisor
        INDIVIDUAL      // Regular employee
    }

    /**
     * Security clearance levels
     */
    public enum ClearanceLevel {
        PUBLIC,       // No clearance required
        INTERNAL,     // Internal employees only
        CONFIDENTIAL, // Confidential clearance
        SECRET,       // Secret clearance
        TOP_SECRET    // Top secret clearance
    }
}
