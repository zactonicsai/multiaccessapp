package com.enterprise.datasharing.repository;

import com.enterprise.datasharing.entity.DataAccessControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for DataAccessControl entity.
 * Provides queries for evaluating access control rules.
 */
@Repository
public interface DataAccessControlRepository extends JpaRepository<DataAccessControl, UUID> {

    /**
     * Find all active rules for an entity
     */
    @Query("""
        SELECT d FROM DataAccessControl d 
        WHERE d.entityType = :entityType 
        AND (d.entityId = :entityId OR d.entityId IS NULL)
        AND d.isActive = true
        AND (d.validFrom IS NULL OR d.validFrom <= :now)
        AND (d.validUntil IS NULL OR d.validUntil >= :now)
        ORDER BY d.priority ASC
        """)
    List<DataAccessControl> findActiveRulesForEntity(
        @Param("entityType") String entityType,
        @Param("entityId") String entityId,
        @Param("now") LocalDateTime now
    );

    /**
     * Find active rules for entity (using current time)
     */
    default List<DataAccessControl> findActiveRulesForEntity(String entityType, String entityId) {
        return findActiveRulesForEntity(entityType, entityId, LocalDateTime.now());
    }

    /**
     * Find row-level rules for a specific user and entity
     */
    @Query("""
        SELECT d FROM DataAccessControl d 
        WHERE d.entityType = :entityType 
        AND d.entityId = :entityId
        AND d.isActive = true
        AND (
            (d.principalType = 'USER' AND d.principalId = :userId)
            OR d.principalType = 'ALL'
        )
        ORDER BY d.priority ASC
        """)
    List<DataAccessControl> findRowLevelRules(
        @Param("entityType") String entityType,
        @Param("entityId") String entityId,
        @Param("userId") String userId
    );

    /**
     * Find column-level rules for a user
     */
    @Query("""
        SELECT d FROM DataAccessControl d 
        WHERE d.entityType = :entityType 
        AND d.isActive = true
        AND (d.allowedColumns IS NOT NULL OR d.deniedColumns IS NOT NULL)
        AND (
            (d.principalType = 'USER' AND d.principalId = :userId)
            OR d.principalType = 'ALL'
        )
        ORDER BY d.priority ASC
        """)
    List<DataAccessControl> findColumnLevelRules(
        @Param("entityType") String entityType,
        @Param("userId") String userId
    );

    /**
     * Find rules by principal type and ID
     */
    List<DataAccessControl> findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(
        DataAccessControl.PrincipalType principalType,
        String principalId
    );

    /**
     * Find rules for a role
     */
    default List<DataAccessControl> findRulesForRole(String role) {
        return findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(
            DataAccessControl.PrincipalType.ROLE, role);
    }

    /**
     * Find rules for a department
     */
    default List<DataAccessControl> findRulesForDepartment(String departmentId) {
        return findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(
            DataAccessControl.PrincipalType.DEPARTMENT, departmentId);
    }

    /**
     * Find rules for a team
     */
    default List<DataAccessControl> findRulesForTeam(String teamId) {
        return findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(
            DataAccessControl.PrincipalType.TEAM, teamId);
    }

    /**
     * Find all rules created by a user
     */
    List<DataAccessControl> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Find expired rules
     */
    @Query("SELECT d FROM DataAccessControl d WHERE d.validUntil < :now AND d.isActive = true")
    List<DataAccessControl> findExpiredRules(@Param("now") LocalDateTime now);
}
