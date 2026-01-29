package com.enterprise.datasharing.repository;

import com.enterprise.datasharing.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity with comprehensive query methods
 * for security monitoring and compliance.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    /**
     * Find audit logs by entity
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEntity(
        @Param("entityType") String entityType,
        @Param("entityId") String entityId,
        Pageable pageable
    );

    /**
     * Find audit logs by action type
     */
    Page<AuditLog> findByActionOrderByTimestampDesc(AuditLog.AuditAction action, Pageable pageable);

    /**
     * Find failed access attempts
     */
    @Query("SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.timestamp DESC")
    Page<AuditLog> findFailedAttempts(Pageable pageable);

    /**
     * Find access denied events
     */
    @Query("SELECT a FROM AuditLog a WHERE a.accessDecision IN ('DENIED_ROLE', 'DENIED_ATTRIBUTE', 'DENIED_CONTEXT', 'DENIED_ROW_LEVEL', 'DENIED_COLUMN_LEVEL') ORDER BY a.timestamp DESC")
    Page<AuditLog> findAccessDeniedEvents(Pageable pageable);

    /**
     * Find by time range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    Page<AuditLog> findByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * Find by correlation ID (for request tracing)
     */
    List<AuditLog> findByCorrelationIdOrderByTimestampAsc(String correlationId);

    /**
     * Count actions by type for a time period
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since GROUP BY a.action")
    List<Object[]> countActionsSince(@Param("since") LocalDateTime since);

    /**
     * Find suspicious activity - multiple failed attempts from same IP
     */
    @Query("""
        SELECT a.ipAddress, COUNT(a) as attempts 
        FROM AuditLog a 
        WHERE a.success = false 
        AND a.timestamp >= :since 
        GROUP BY a.ipAddress 
        HAVING COUNT(a) >= :threshold 
        ORDER BY attempts DESC
        """)
    List<Object[]> findSuspiciousIps(
        @Param("since") LocalDateTime since,
        @Param("threshold") long threshold
    );

    /**
     * Find user activity summary
     */
    @Query("""
        SELECT a.userId, a.username, a.action, COUNT(a) 
        FROM AuditLog a 
        WHERE a.timestamp >= :since 
        GROUP BY a.userId, a.username, a.action
        """)
    List<Object[]> getUserActivitySummary(@Param("since") LocalDateTime since);

    /**
     * Find data export events (for compliance)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action = 'EXPORT' ORDER BY a.timestamp DESC")
    Page<AuditLog> findExportEvents(Pageable pageable);

    /**
     * Delete old audit logs (for retention policy)
     */
    void deleteByTimestampBefore(LocalDateTime cutoff);
}
