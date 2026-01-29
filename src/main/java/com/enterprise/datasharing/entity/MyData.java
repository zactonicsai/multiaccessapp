package com.enterprise.datasharing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Main data entity that stores business data.
 * Subject to RBAC, ABAC, and CBAC access controls.
 */
@Entity
@Table(name = "mydata", indexes = {
    @Index(name = "idx_mydata_owner", columnList = "owner_id"),
    @Index(name = "idx_mydata_org_level", columnList = "organization_level"),
    @Index(name = "idx_mydata_department", columnList = "department_id"),
    @Index(name = "idx_mydata_team", columnList = "team_id"),
    @Index(name = "idx_mydata_date", columnList = "data_date")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "data_date", nullable = false)
    private LocalDate dataDate;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    // Sensitivity classification for column-level access control
    @Enumerated(EnumType.STRING)
    @Column(name = "sensitivity_level", nullable = false)
    @Builder.Default
    private SensitivityLevel sensitivityLevel = SensitivityLevel.INTERNAL;

    // Organization hierarchy fields for RBAC/ABAC
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_level", nullable = false)
    private OrganizationLevel organizationLevel;

    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "team_id")
    private String teamId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    // Confidential data - requires special clearance
    @Column(name = "confidential_notes", columnDefinition = "TEXT")
    private String confidentialNotes;

    // Financial data - restricted access
    @Column(name = "financial_data")
    private String financialData;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Version
    private Long version;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    /**
     * Organization levels for data sharing hierarchy
     */
    public enum OrganizationLevel {
        EXECUTIVE,      // Visible to all executives across organization
        DEPARTMENT,     // Visible to department members
        TEAM,           // Visible to team members only
        INDIVIDUAL      // Visible only to owner
    }

    /**
     * Sensitivity levels for column-level access control
     */
    public enum SensitivityLevel {
        PUBLIC,         // Anyone can access
        INTERNAL,       // Internal employees only
        CONFIDENTIAL,   // Requires confidential clearance
        RESTRICTED      // Requires restricted clearance (need-to-know basis)
    }
}
