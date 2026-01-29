package com.enterprise.datasharing.repository;

import com.enterprise.datasharing.entity.MyData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MyData entity with support for dynamic queries
 * and organization-level filtering.
 */
@Repository
public interface MyDataRepository extends JpaRepository<MyData, UUID>, JpaSpecificationExecutor<MyData> {

    /**
     * Find by ID excluding soft-deleted records
     */
    @Query("SELECT d FROM MyData d WHERE d.id = :id AND d.isDeleted = false")
    Optional<MyData> findByIdAndNotDeleted(@Param("id") UUID id);

    /**
     * Find all non-deleted records
     */
    @Query("SELECT d FROM MyData d WHERE d.isDeleted = false")
    Page<MyData> findAllActive(Pageable pageable);

    /**
     * Find by owner
     */
    @Query("SELECT d FROM MyData d WHERE d.ownerId = :ownerId AND d.isDeleted = false")
    List<MyData> findByOwnerId(@Param("ownerId") String ownerId);

    /**
     * Find records accessible at executive level
     */
    @Query("SELECT d FROM MyData d WHERE d.organizationLevel = 'EXECUTIVE' AND d.isDeleted = false")
    Page<MyData> findExecutiveLevelData(Pageable pageable);

    /**
     * Find records for a specific department
     */
    @Query("SELECT d FROM MyData d WHERE d.departmentId = :departmentId " +
           "AND d.organizationLevel IN ('DEPARTMENT', 'TEAM', 'INDIVIDUAL') " +
           "AND d.isDeleted = false")
    Page<MyData> findByDepartment(@Param("departmentId") String departmentId, Pageable pageable);

    /**
     * Find records for a specific team
     */
    @Query("SELECT d FROM MyData d WHERE d.teamId = :teamId " +
           "AND d.organizationLevel IN ('TEAM', 'INDIVIDUAL') " +
           "AND d.isDeleted = false")
    Page<MyData> findByTeam(@Param("teamId") String teamId, Pageable pageable);

    /**
     * Find records accessible by a user based on organization hierarchy
     */
    @Query("""
        SELECT d FROM MyData d WHERE d.isDeleted = false AND (
            d.ownerId = :userId
            OR (d.organizationLevel = 'EXECUTIVE' AND :isExecutive = true)
            OR (d.organizationLevel = 'DEPARTMENT' AND d.departmentId = :departmentId)
            OR (d.organizationLevel = 'TEAM' AND d.teamId = :teamId)
            OR (d.organizationLevel = 'INDIVIDUAL' AND d.ownerId = :userId)
        )
        """)
    Page<MyData> findAccessibleByUser(
        @Param("userId") String userId,
        @Param("departmentId") String departmentId,
        @Param("teamId") String teamId,
        @Param("isExecutive") boolean isExecutive,
        Pageable pageable
    );

    /**
     * Find by date range
     */
    @Query("SELECT d FROM MyData d WHERE d.dataDate BETWEEN :startDate AND :endDate AND d.isDeleted = false")
    List<MyData> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Search by name (case-insensitive)
     */
    @Query("SELECT d FROM MyData d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) AND d.isDeleted = false")
    Page<MyData> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Count by organization level
     */
    @Query("SELECT d.organizationLevel, COUNT(d) FROM MyData d WHERE d.isDeleted = false GROUP BY d.organizationLevel")
    List<Object[]> countByOrganizationLevel();

    /**
     * Find by sensitivity level
     */
    List<MyData> findBySensitivityLevelAndIsDeletedFalse(MyData.SensitivityLevel sensitivityLevel);
}
