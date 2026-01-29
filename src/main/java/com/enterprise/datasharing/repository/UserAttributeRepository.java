package com.enterprise.datasharing.repository;

import com.enterprise.datasharing.entity.UserAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserAttribute entity.
 * Provides queries for ABAC attribute lookups.
 */
@Repository
public interface UserAttributeRepository extends JpaRepository<UserAttribute, Long> {

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
    List<UserAttribute> findByDepartmentAndActiveTrue(String department);

    /**
     * Find all users in a team
     */
    List<UserAttribute> findByTeamAndActiveTrue(String team);

    /**
     * Find all executives
     */
    List<UserAttribute> findByIsExecutiveTrueAndActiveTrue();

    /**
     * Find all department heads
     */
    List<UserAttribute> findByIsDepartmentHeadTrueAndActiveTrue();

    /**
     * Find all managers
     */
    List<UserAttribute> findByIsManagerTrueAndActiveTrue();

    /**
     * Find users by organization level
     */
    List<UserAttribute> findByOrganizationLevelAndActiveTrue(UserAttribute.OrganizationLevel level);

    /**
     * Find users by clearance level
     */
    List<UserAttribute> findByClearanceLevelAndActiveTrue(UserAttribute.ClearanceLevel level);

    /**
     * Find users with clearance level at or above specified level
     */
    @Query("SELECT u FROM UserAttribute u WHERE u.clearanceLevel >= :level AND u.active = true")
    List<UserAttribute> findByMinClearanceLevel(@Param("level") UserAttribute.ClearanceLevel level);

    /**
     * Find direct reports of a manager
     */
    List<UserAttribute> findByManagerIdAndActiveTrue(String managerId);

    /**
     * Check if user exists
     */
    boolean existsByUserId(String userId);

    /**
     * Find all users in management chain of a user
     */
    @Query(value = """
        WITH RECURSIVE management_chain AS (
            SELECT * FROM user_attribute WHERE user_id = :userId
            UNION ALL
            SELECT ua.* FROM user_attribute ua
            JOIN management_chain mc ON ua.user_id = mc.manager_id
        )
        SELECT * FROM management_chain WHERE user_id != :userId
        """, nativeQuery = true)
    List<UserAttribute> findManagementChain(@Param("userId") String userId);
}
