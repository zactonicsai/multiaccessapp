package com.enterprise.datasharing.repository;

import com.enterprise.datasharing.entity.UserAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserAttribute entity.
 * Provides queries for ABAC attribute lookups.
 */
@Repository
public interface UserAttributeRepository extends JpaRepository<UserAttribute, UUID> {

    /**
     * Find by user ID
     */
    Optional<UserAttribute> findByUserId(String userId);

    /**
     * Find by username
     */
    Optional<UserAttribute> findByUsername(String username);

    /**
     * Find all users in a department
     */
    List<UserAttribute> findByDepartmentIdAndIsActiveTrue(String departmentId);

    /**
     * Find all users in a team
     */
    List<UserAttribute> findByTeamIdAndIsActiveTrue(String teamId);

    /**
     * Find all executives
     */
    List<UserAttribute> findByIsExecutiveTrueAndIsActiveTrue();

    /**
     * Find all department heads
     */
    List<UserAttribute> findByIsDepartmentHeadTrueAndIsActiveTrue();

    /**
     * Find all managers
     */
    List<UserAttribute> findByIsManagerTrueAndIsActiveTrue();

    /**
     * Find users by organization level
     */
    List<UserAttribute> findByOrganizationLevelAndIsActiveTrue(UserAttribute.OrganizationLevel level);

    /**
     * Find users by clearance level
     */
    List<UserAttribute> findByClearanceLevelAndIsActiveTrue(UserAttribute.ClearanceLevel level);

    /**
     * Find users with clearance level at or above specified level
     */
    @Query("SELECT u FROM UserAttribute u WHERE u.clearanceLevel >= :level AND u.isActive = true")
    List<UserAttribute> findByMinClearanceLevel(@Param("level") UserAttribute.ClearanceLevel level);

    /**
     * Find direct reports of a manager
     */
    List<UserAttribute> findByManagerIdAndIsActiveTrue(String managerId);

    /**
     * Check if user exists
     */
    boolean existsByUserId(String userId);

    /**
     * Find all users in management chain of a user
     */
    @Query(value = """
        WITH RECURSIVE management_chain AS (
            SELECT * FROM user_attributes WHERE user_id = :userId
            UNION ALL
            SELECT ua.* FROM user_attributes ua
            JOIN management_chain mc ON ua.user_id = mc.manager_id
        )
        SELECT * FROM management_chain WHERE user_id != :userId
        """, nativeQuery = true)
    List<UserAttribute> findManagementChain(@Param("userId") String userId);
}
