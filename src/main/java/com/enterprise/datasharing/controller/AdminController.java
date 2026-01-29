package com.enterprise.datasharing.controller;

import com.enterprise.datasharing.entity.AuditLog;
import com.enterprise.datasharing.entity.DataAccessControl;
import com.enterprise.datasharing.entity.UserAttribute;
import com.enterprise.datasharing.repository.AuditLogRepository;
import com.enterprise.datasharing.repository.DataAccessControlRepository;
import com.enterprise.datasharing.repository.UserAttributeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


/**
 * Admin Controller for managing access control rules, user attributes, and viewing audit logs.
 * Requires ADMIN role for all operations.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "Administrative operations for access control and auditing")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DataAccessControlRepository accessControlRepository;
    private final UserAttributeRepository userAttributeRepository;
    private final AuditLogRepository auditLogRepository;

    // ==================== Access Control Management ====================

    /**
     * Create new access control rule
     */
    @PostMapping("/access-rules")
    @Operation(summary = "Create access control rule",
        description = "Creates a new access control rule for RBAC/ABAC/CBAC")
    public ResponseEntity<DataAccessControl> createAccessRule(
            @RequestBody DataAccessControl rule) {
        log.info("Creating access rule: {}", rule.getRuleName());
        DataAccessControl saved = accessControlRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Get all access control rules
     */
    @GetMapping("/access-rules")
    @Operation(summary = "List all access control rules")
    public ResponseEntity<List<DataAccessControl>> getAllAccessRules() {
        return ResponseEntity.ok(accessControlRepository.findAll());
    }

    /**
     * Get access rule by ID
     */
    @GetMapping("/access-rules/{id}")
    @Operation(summary = "Get access control rule by ID")
    public ResponseEntity<DataAccessControl> getAccessRule(@PathVariable Long id) {
        return accessControlRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update access control rule
     */
    @PutMapping("/access-rules/{id}")
    @Operation(summary = "Update access control rule")
    public ResponseEntity<DataAccessControl> updateAccessRule(
            @PathVariable Long id,
            @RequestBody DataAccessControl rule) {
        return accessControlRepository.findById(id)
            .map(existing -> {
                rule.setId(id);
                rule.setCreatedAt(existing.getCreatedAt());
                rule.setCreatedBy(existing.getCreatedBy());
                return ResponseEntity.ok(accessControlRepository.save(rule));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete access control rule
     */
    @DeleteMapping("/access-rules/{id}")
    @Operation(summary = "Delete access control rule")
    public ResponseEntity<Void> deleteAccessRule(@PathVariable Long id) {
        if (accessControlRepository.existsById(id)) {
            accessControlRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get rules for a specific role
     */
    @GetMapping("/access-rules/by-role/{role}")
    @Operation(summary = "Get access rules for a role")
    public ResponseEntity<List<DataAccessControl>> getRulesForRole(@PathVariable String role) {
        return ResponseEntity.ok(accessControlRepository.findRulesForRole(role));
    }

    /**
     * Get rules for a specific department
     */
    @GetMapping("/access-rules/by-department/{departmentId}")
    @Operation(summary = "Get access rules for a department")
    public ResponseEntity<List<DataAccessControl>> getRulesForDepartment(
            @PathVariable String departmentId) {
        return ResponseEntity.ok(accessControlRepository.findRulesForDepartment(departmentId));
    }

    // ==================== User Attribute Management ====================

    /**
     * Create or update user attributes
     */
    @PostMapping("/user-attributes")
    @Operation(summary = "Create or update user attributes",
        description = "Manages user attributes for ABAC decisions")
    public ResponseEntity<UserAttribute> saveUserAttribute(@RequestBody UserAttribute attribute) {
        log.info("Saving user attributes for: {}", attribute.getUserId());
        
        // Check if exists and preserve audit fields
        userAttributeRepository.findByUserId(attribute.getUserId())
            .ifPresent(existing -> {
                attribute.setId(existing.getId());
                attribute.setCreatedAt(existing.getCreatedAt());
            });

        UserAttribute saved = userAttributeRepository.save(attribute);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get all user attributes
     */
    @GetMapping("/user-attributes")
    @Operation(summary = "List all user attributes")
    public ResponseEntity<List<UserAttribute>> getAllUserAttributes() {
        return ResponseEntity.ok(userAttributeRepository.findAll());
    }

    /**
     * Get user attribute by user ID
     */
    @GetMapping("/user-attributes/{userId}")
    @Operation(summary = "Get user attributes by user ID")
    public ResponseEntity<UserAttribute> getUserAttribute(@PathVariable String userId) {
        return userAttributeRepository.findByUserId(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get users by department
     */
    @GetMapping("/user-attributes/by-department/{departmentId}")
    @Operation(summary = "Get users in a department")
    public ResponseEntity<List<UserAttribute>> getUsersByDepartment(
            @PathVariable String departmentId) {
        return ResponseEntity.ok(
            userAttributeRepository.findByDepartmentAndActiveTrue(departmentId));
    }

    /**
     * Get users by team
     */
    @GetMapping("/user-attributes/by-team/{teamId}")
    @Operation(summary = "Get users in a team")
    public ResponseEntity<List<UserAttribute>> getUsersByTeam(@PathVariable String teamId) {
        return ResponseEntity.ok(
            userAttributeRepository.findByTeamAndActiveTrue(teamId));
    }

    /**
     * Get executives
     */
    @GetMapping("/user-attributes/executives")
    @Operation(summary = "Get all executives")
    public ResponseEntity<List<UserAttribute>> getExecutives() {
        return ResponseEntity.ok(userAttributeRepository.findByIsExecutiveTrueAndActiveTrue());
    }

    /**
     * Get users by clearance level
     */
    @GetMapping("/user-attributes/by-clearance/{level}")
    @Operation(summary = "Get users by clearance level")
    public ResponseEntity<List<UserAttribute>> getUsersByClearance(
            @PathVariable UserAttribute.ClearanceLevel level) {
        return ResponseEntity.ok(
            userAttributeRepository.findByClearanceLevelAndActiveTrue(level));
    }

    // ==================== Audit Log Viewing ====================

    /**
     * Get all audit logs
     */
    @GetMapping("/audit-logs")
    @Operation(summary = "List audit logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    /**
     * Get audit logs by user
     */
    @GetMapping("/audit-logs/by-user/{userId}")
    @Operation(summary = "Get audit logs for a user")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @PathVariable String userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
            auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable));
    }

    /**
     * Get audit logs by entity
     */
    @GetMapping("/audit-logs/by-entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for an entity")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
            auditLogRepository.findByEntity(entityType, entityId, pageable));
    }

    /**
     * Get audit logs by action
     */
    @GetMapping("/audit-logs/by-action/{action}")
    @Operation(summary = "Get audit logs by action type")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByAction(
            @PathVariable AuditLog.AuditAction action,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
            auditLogRepository.findByActionOrderByTimestampDesc(action, pageable));
    }

    /**
     * Get failed access attempts
     */
    @GetMapping("/audit-logs/failed")
    @Operation(summary = "Get failed access attempts")
    public ResponseEntity<Page<AuditLog>> getFailedAttempts(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findFailedAttempts(pageable));
    }

    /**
     * Get access denied events
     */
    @GetMapping("/audit-logs/access-denied")
    @Operation(summary = "Get access denied events")
    public ResponseEntity<Page<AuditLog>> getAccessDeniedEvents(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findAccessDeniedEvents(pageable));
    }

    /**
     * Get audit logs by time range
     */
    @GetMapping("/audit-logs/time-range")
    @Operation(summary = "Get audit logs within time range")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByTimeRange(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
            auditLogRepository.findByTimeRange(startTime, endTime, pageable));
    }

    /**
     * Get action statistics
     */
    @GetMapping("/audit-logs/stats")
    @Operation(summary = "Get audit action statistics")
    public ResponseEntity<List<Object[]>> getActionStats(
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(auditLogRepository.countActionsSince(since));
    }

    /**
     * Get suspicious IP addresses
     */
    @GetMapping("/audit-logs/suspicious-ips")
    @Operation(summary = "Get suspicious IP addresses with many failed attempts")
    public ResponseEntity<List<Object[]>> getSuspiciousIps(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "5") long threshold) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(
            auditLogRepository.findSuspiciousIps(since, threshold));
    }

    /**
     * Get user activity summary
     */
    @GetMapping("/audit-logs/user-activity")
    @Operation(summary = "Get user activity summary")
    public ResponseEntity<List<Object[]>> getUserActivitySummary(
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(auditLogRepository.getUserActivitySummary(since));
    }

    /**
     * Get audit log by correlation ID (request tracing)
     */
    @GetMapping("/audit-logs/correlation/{correlationId}")
    @Operation(summary = "Get audit logs by correlation ID for request tracing")
    public ResponseEntity<List<AuditLog>> getByCorrelationId(
            @PathVariable String correlationId) {
        return ResponseEntity.ok(
            auditLogRepository.findByCorrelationIdOrderByTimestampAsc(correlationId));
    }
}
