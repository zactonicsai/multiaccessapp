package com.enterprise.datasharing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Main data entity that stores business data.
 * Subject to RBAC, ABAC, and CBAC access controls.
 */
@Entity
@Table(name = "my_data", indexes = {
    @Index(name = "idx_my_data_owner", columnList = "owner_id"),
    @Index(name = "idx_my_data_org_level", columnList = "organization_level"),
    @Index(name = "idx_my_data_department", columnList = "owner_department"),
    @Index(name = "idx_my_data_team", columnList = "owner_team"),
    @Index(name = "idx_my_data_date", columnList = "date")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    // Sensitivity classification for column-level access control
    @Enumerated(EnumType.STRING)
    @Column(name = "sensitivity_level", nullable = false, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private SensitivityLevel sensitivityLevel = SensitivityLevel.INTERNAL;

    // Organization hierarchy fields for RBAC/ABAC
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_level", nullable = false, columnDefinition = "VARCHAR(50)")
    @Builder.Default
    private OrganizationLevel organizationLevel = OrganizationLevel.INDIVIDUAL;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "owner_department", length = 100)
    private String ownerDepartment;

    @Column(name = "owner_team", length = 100)
    private String ownerTeam;

    // Confidential data - requires special clearance
    @Column(name = "confidential_notes", columnDefinition = "TEXT")
    private String confidentialNotes;

    // Financial data - restricted access (stored as JSON string)
    @Column(name = "financial_data", columnDefinition = "TEXT")
    private String financialData;

    // Metadata (stored as JSON string)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    // Soft delete fields
    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Version
    @Column(name = "version")
    private Long version;

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
