package com.enterprise.datasharing.service;

import com.enterprise.datasharing.entity.AuditLog;
import com.enterprise.datasharing.repository.AuditLogRepository;
import com.enterprise.datasharing.security.AccessDecision;
import com.enterprise.datasharing.security.SecurityContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service for comprehensive audit logging.
 * Logs all data access, modifications, and security events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log a data access or modification event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logDataAccess(
            SecurityContext context,
            AuditLog.AuditAction action,
            String entityType,
            String entityId,
            Object oldValue,
            Object newValue,
            AccessDecision accessDecision,
            boolean success,
            String errorMessage) {

        HttpServletRequest request = getCurrentRequest();
        String correlationId = getCorrelationId(request);

        AuditLog auditLog = AuditLog.builder()
            .userId(context.getUserId())
            .username(context.getUsername())
            .userRoles(String.join(",", context.getRoles()))
            .userDepartment(context.getDepartmentId())
            .userTeam(context.getTeamId())
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .oldValue(serializeValue(oldValue))
            .newValue(serializeValue(newValue))
            .timestamp(LocalDateTime.now())
            .ipAddress(getClientIp(request))
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .requestUri(request != null ? request.getRequestURI() : null)
            .httpMethod(request != null ? request.getMethod() : null)
            .accessDecision(accessDecision != null ? accessDecision.getDenialReason() : 
                (success ? AuditLog.AccessDecision.GRANTED : null))
            .accessReason(accessDecision != null ? accessDecision.buildAuditSummary() : null)
            .correlationId(correlationId)
            .sessionId(getSessionId(request))
            .success(success)
            .errorMessage(errorMessage)
            .dataHash(computeHash(newValue))
            .build();

        // Add RBAC/ABAC/CBAC details if available
        if (accessDecision != null) {
            if (accessDecision.getRbacResult() != null) {
                auditLog.setRequiredRole(accessDecision.getRbacResult().getRequiredRole());
            }
            if (accessDecision.getAbacResult() != null) {
                auditLog.setAttributeConditions(
                    serializeValue(accessDecision.getAbacResult().getEvaluatedAttributes()));
            }
            if (accessDecision.getCbacResult() != null) {
                auditLog.setContextConditions(
                    serializeValue(accessDecision.getCbacResult().getEvaluatedContext()));
            }
        }

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} - {} on {}/{}", 
            action, context.getUsername(), entityType, entityId);

        return saved;
    }

    /**
     * Log field-level change
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFieldChange(
            SecurityContext context,
            String entityType,
            String entityId,
            String fieldName,
            Object oldValue,
            Object newValue) {

        HttpServletRequest request = getCurrentRequest();

        AuditLog auditLog = AuditLog.builder()
            .userId(context.getUserId())
            .username(context.getUsername())
            .userRoles(String.join(",", context.getRoles()))
            .action(AuditLog.AuditAction.UPDATE)
            .entityType(entityType)
            .entityId(entityId)
            .fieldName(fieldName)
            .oldValue(serializeValue(oldValue))
            .newValue(serializeValue(newValue))
            .timestamp(LocalDateTime.now())
            .ipAddress(getClientIp(request))
            .correlationId(getCorrelationId(request))
            .success(true)
            .accessDecision(AuditLog.AccessDecision.GRANTED)
            .build();

        auditLogRepository.save(auditLog);
    }

    /**
     * Log access denied event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAccessDenied(
            SecurityContext context,
            String entityType,
            String entityId,
            AuditLog.AuditAction attemptedAction,
            AccessDecision decision) {

        HttpServletRequest request = getCurrentRequest();

        AuditLog auditLog = AuditLog.builder()
            .userId(context.getUserId())
            .username(context.getUsername())
            .userRoles(String.join(",", context.getRoles()))
            .userDepartment(context.getDepartmentId())
            .userTeam(context.getTeamId())
            .action(AuditLog.AuditAction.ACCESS_DENIED)
            .entityType(entityType)
            .entityId(entityId)
            .timestamp(LocalDateTime.now())
            .ipAddress(getClientIp(request))
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .requestUri(request != null ? request.getRequestURI() : null)
            .accessDecision(decision.getDenialReason())
            .accessReason(decision.getDenialDetails())
            .correlationId(getCorrelationId(request))
            .success(false)
            .build();

        auditLogRepository.save(auditLog);
        log.warn("Access denied: User {} attempted {} on {}/{} - Reason: {}", 
            context.getUsername(), attemptedAction, entityType, entityId, 
            decision.getDenialDetails());
    }

    /**
     * Log authentication event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthentication(String userId, String username, AuditLog.AuditAction action, boolean success) {
        HttpServletRequest request = getCurrentRequest();

        AuditLog auditLog = AuditLog.builder()
            .userId(userId)
            .username(username)
            .action(action)
            .entityType("AUTHENTICATION")
            .timestamp(LocalDateTime.now())
            .ipAddress(getClientIp(request))
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .success(success)
            .accessDecision(success ? AuditLog.AccessDecision.GRANTED : AuditLog.AccessDecision.DENIED_ROLE)
            .build();

        auditLogRepository.save(auditLog);
    }

    /**
     * Async log for non-critical events
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(
            SecurityContext context,
            AuditLog.AuditAction action,
            String entityType,
            String entityId,
            String details) {

        AuditLog auditLog = AuditLog.builder()
            .userId(context.getUserId())
            .username(context.getUsername())
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .accessReason(details)
            .timestamp(LocalDateTime.now())
            .success(true)
            .build();

        auditLogRepository.save(auditLog);
    }

    private String serializeValue(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return value.toString();
        }
    }

    private String computeHash(Object value) {
        if (value == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(serializeValue(value).getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getCorrelationId(HttpServletRequest request) {
        if (request == null) return UUID.randomUUID().toString();
        String correlationId = request.getHeader("X-Correlation-ID");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private String getSessionId(HttpServletRequest request) {
        if (request == null) return null;
        return request.getSession(false) != null ? request.getSession().getId() : null;
    }
}
