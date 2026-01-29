package com.enterprise.datasharing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Attribute entity for ABAC (Attribute-Based Access Control).
 * Stores additional user attributes beyond what Keycloak provides.
 * These attributes are used to make access control decisions.
 */
@Entity
@Table(name = "user_attributes", indexes = {
    @Index(name = "idx_user_attr_user_id", columnList = "user_id", unique = true),
    @Index(name = "idx_user_attr_department", columnList = "department_id"),
    @Index(name = "idx_user_attr_team", columnList = "team_id"),
    @Index(name = "idx_user_attr_clearance", columnList = "clearance_level")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email")
    private String email;

    // Organizational hierarchy
    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "team_id")
    private String teamId;

    @Column(name = "team_name")
    private String teamName;

    // Organization level (for RBAC)
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_level", nullable = false)
    @Builder.Default
    private OrganizationLevel organizationLevel = OrganizationLevel.INDIVIDUAL;

    // Clearance levels (for ABAC)
    @Enumerated(EnumType.STRING)
    @Column(name = "clearance_level", nullable = false)
    @Builder.Default
    private ClearanceLevel clearanceLevel = ClearanceLevel.PUBLIC;

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

    // Employment details
    @Column(name = "employee_type")
    private String employeeType;  // FULL_TIME, CONTRACTOR, etc.

    @Column(name = "hire_date")
    private LocalDateTime hireDate;

    // Additional custom attributes (JSON)
    @Column(name = "custom_attributes", columnDefinition = "TEXT")
    private String customAttributes;

    // Status
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /**
     * Organization levels for hierarchy-based access
     */
    public enum OrganizationLevel {
        EXECUTIVE,          // C-level, VP, Director
        DEPARTMENT_MANAGER, // Department/Division head
        TEAM_LEAD,          // Team lead/supervisor
        INDIVIDUAL          // Regular employee
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
