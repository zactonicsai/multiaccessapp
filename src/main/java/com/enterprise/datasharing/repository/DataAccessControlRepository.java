package com.enterprise.datasharing.repository;

import com.enterprise.datasharing.entity.DataAccessControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository for DataAccessControl entity.
 * Provides queries for evaluating access control rules.
 */
@Repository
public interface DataAccessControlRepository extends JpaRepository<DataAccessControl, Long> {

    /**
     * Find all active rules for a data record
     */
    @Query("""
        SELECT d FROM DataAccessControl d 
        WHERE (d.dataId = :dataId OR d.dataId IS NULL)
        AND d.active = true
        AND (d.validFrom IS NULL OR d.validFrom <= :now)
        AND (d.validUntil IS NULL OR d.validUntil >= :now)
        ORDER BY d.priority DESC
        """)
    List<DataAccessControl> findActiveRulesForData(
        @Param("dataId") Long dataId,
        @Param("now") OffsetDateTime now
    );

    /**
     * Find active rules for data (using current time)
     */
    default List<DataAccessControl> findActiveRulesForData(Long dataId) {
        return findActiveRulesForData(dataId, OffsetDateTime.now());
    }

    /**
     * Find row-level rules for a specific user and data
     */
    @Query("""
        SELECT d FROM DataAccessControl d 
        WHERE d.dataId = :dataId
        AND d.active = true
        AND (
            (d.principalType = 'USER' AND d.principalValue = :userId)
            OR d.principalType = 'ALL'
        )
        ORDER BY d.priority DESC
        """)
    List<DataAccessControl> findRowLevelRules(
        @Param("dataId") Long dataId,
        @Param("userId") String userId
    );

    /**
     * Find column-level rules for a user
     */
    @Query("""
        SELECT d FROM DataAccessControl d 
        WHERE d.active = true
        AND d.visibleColumns IS NOT NULL
        AND (
            (d.principalType = 'USER' AND d.principalValue = :userId)
            OR d.principalType = 'ALL'
        )
        ORDER BY d.priority DESC
        """)
    List<DataAccessControl> findColumnLevelRules(
        @Param("userId") String userId
    );

    /**
     * Find rules by principal type and value
     */
    List<DataAccessControl> findByPrincipalTypeAndPrincipalValueAndActiveTrue(
        DataAccessControl.PrincipalType principalType,
        String principalValue
    );

    /**
     * Find rules for a role
     */
    default List<DataAccessControl> findRulesForRole(String role) {
        return findByPrincipalTypeAndPrincipalValueAndActiveTrue(
            DataAccessControl.PrincipalType.ROLE, role);
    }

    /**
     * Find rules for a department
     */
    default List<DataAccessControl> findRulesForDepartment(String department) {
        return findByPrincipalTypeAndPrincipalValueAndActiveTrue(
            DataAccessControl.PrincipalType.DEPARTMENT, department);
    }

    /**
     * Find rules for a team
     */
    default List<DataAccessControl> findRulesForTeam(String team) {
        return findByPrincipalTypeAndPrincipalValueAndActiveTrue(
            DataAccessControl.PrincipalType.TEAM, team);
    }

    /**
     * Find rules for a clearance level
     */
    default List<DataAccessControl> findRulesForClearance(String clearance) {
        return findByPrincipalTypeAndPrincipalValueAndActiveTrue(
            DataAccessControl.PrincipalType.CLEARANCE, clearance);
    }

    /**
     * Find all rules created by a user
     */
    List<DataAccessControl> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Find all active rules
     */
    List<DataAccessControl> findByActiveTrueOrderByPriorityDesc();

    /**
     * Find expired rules
     */
    @Query("SELECT d FROM DataAccessControl d WHERE d.validUntil < :now AND d.active = true")
    List<DataAccessControl> findExpiredRules(@Param("now") OffsetDateTime now);
}
