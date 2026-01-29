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

/**
 * Repository for MyData entity with support for dynamic queries
 * and organization-level filtering.
 */
@Repository
public interface MyDataRepository extends JpaRepository<MyData, Long>, JpaSpecificationExecutor<MyData> {

    /**
     * Find by ID excluding soft-deleted records
     */
    @Query("SELECT d FROM MyData d WHERE d.id = :id AND d.deleted = false")
    Optional<MyData> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find all non-deleted records
     */
    @Query("SELECT d FROM MyData d WHERE d.deleted = false")
    Page<MyData> findAllActive(Pageable pageable);

    /**
     * Find by owner
     */
    @Query("SELECT d FROM MyData d WHERE d.ownerId = :ownerId AND d.deleted = false")
    List<MyData> findByOwnerId(@Param("ownerId") String ownerId);

    /**
     * Find records accessible at executive level
     */
    @Query("SELECT d FROM MyData d WHERE d.organizationLevel = 'EXECUTIVE' AND d.deleted = false")
    Page<MyData> findExecutiveLevelData(Pageable pageable);

    /**
     * Find records for a specific department
     */
    @Query("SELECT d FROM MyData d WHERE d.ownerDepartment = :department " +
           "AND d.organizationLevel IN ('DEPARTMENT', 'TEAM', 'INDIVIDUAL') " +
           "AND d.deleted = false")
    Page<MyData> findByDepartment(@Param("department") String department, Pageable pageable);

    /**
     * Find records for a specific team
     */
    @Query("SELECT d FROM MyData d WHERE d.ownerTeam = :team " +
           "AND d.organizationLevel IN ('TEAM', 'INDIVIDUAL') " +
           "AND d.deleted = false")
    Page<MyData> findByTeam(@Param("team") String team, Pageable pageable);

    /**
     * Find records accessible by a user based on organization hierarchy
     */
    @Query("""
        SELECT d FROM MyData d WHERE d.deleted = false AND (
            d.ownerId = :userId
            OR (d.organizationLevel = 'EXECUTIVE' AND :isExecutive = true)
            OR (d.organizationLevel = 'DEPARTMENT' AND d.ownerDepartment = :department)
            OR (d.organizationLevel = 'TEAM' AND d.ownerTeam = :team)
            OR (d.organizationLevel = 'INDIVIDUAL' AND d.ownerId = :userId)
        )
        """)
    Page<MyData> findAccessibleByUser(
        @Param("userId") String userId,
        @Param("department") String department,
        @Param("team") String team,
        @Param("isExecutive") boolean isExecutive,
        Pageable pageable
    );

    /**
     * Find by date range
     */
    @Query("SELECT d FROM MyData d WHERE d.date BETWEEN :startDate AND :endDate AND d.deleted = false")
    List<MyData> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Search by name (case-insensitive)
     */
    @Query("SELECT d FROM MyData d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) AND d.deleted = false")
    Page<MyData> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Count by organization level
     */
    @Query("SELECT d.organizationLevel, COUNT(d) FROM MyData d WHERE d.deleted = false GROUP BY d.organizationLevel")
    List<Object[]> countByOrganizationLevel();

    /**
     * Find by sensitivity level
     */
    List<MyData> findBySensitivityLevelAndDeletedFalse(MyData.SensitivityLevel sensitivityLevel);
}
