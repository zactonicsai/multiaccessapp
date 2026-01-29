package com.enterprise.datasharing.security;

import com.enterprise.datasharing.entity.AuditLog;
import com.enterprise.datasharing.entity.DataAccessControl;
import com.enterprise.datasharing.entity.MyData;
import com.enterprise.datasharing.entity.UserAttribute;
import com.enterprise.datasharing.repository.DataAccessControlRepository;
import com.enterprise.datasharing.repository.UserAttributeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive Access Control Service implementing:
 * - RBAC (Role-Based Access Control)
 * - ABAC (Attribute-Based Access Control)
 * - CBAC (Context-Based Access Control)
 * - Row-level security
 * - Column-level security
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccessControlService {

    private final DataAccessControlRepository accessControlRepository;
    private final UserAttributeRepository userAttributeRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.security.context.business-hours.start:08:00}")
    private String businessHoursStart;

    @Value("${app.security.context.business-hours.end:18:00}")
    private String businessHoursEnd;

    @Value("${app.security.context.business-hours.timezone:America/New_York}")
    private String timezone;

    @Value("${app.security.context.require-business-hours:false}")
    private boolean requireBusinessHours;

    @Value("${app.security.context.require-allowed-ip:false}")
    private boolean requireAllowedIp;

    @Value("${app.security.context.allowed-ip-ranges:10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,127.0.0.1/32}")
    private List<String> allowedIpRanges;

    @Value("${app.security.row-level.enabled:true}")
    private boolean rowLevelSecurityEnabled;

    @Value("${app.security.column-level.enabled:true}")
    private boolean columnLevelSecurityEnabled;

    /**
     * Main access control decision method.
     * Evaluates RBAC, ABAC, and CBAC rules to determine access.
     */
    public AccessDecision checkAccess(
            SecurityContext securityContext,
            MyData data,
            AccessOperation operation,
            HttpServletRequest request) {

        log.debug("Checking access for user {} on data {} for operation {}",
            securityContext.getUserId(), data.getId(), operation);

        AccessDecision decision = new AccessDecision();
        decision.setUserId(securityContext.getUserId());
        decision.setEntityId(data.getId().toString());
        decision.setOperation(operation);

        // 1. RBAC Check - Role-based access
        AccessDecision.RbacResult rbacResult = checkRbac(securityContext, data, operation);
        decision.setRbacResult(rbacResult);
        if (!rbacResult.isAllowed()) {
            decision.setAllowed(false);
            decision.setDenialReason(AuditLog.AccessDecision.DENIED_ROLE);
            decision.setDenialDetails(rbacResult.getReason());
            return decision;
        }

        // 2. ABAC Check - Attribute-based access
        AccessDecision.AbacResult abacResult = checkAbac(securityContext, data, operation);
        decision.setAbacResult(abacResult);
        if (!abacResult.isAllowed()) {
            decision.setAllowed(false);
            decision.setDenialReason(AuditLog.AccessDecision.DENIED_ATTRIBUTE);
            decision.setDenialDetails(abacResult.getReason());
            return decision;
        }

        // 3. CBAC Check - Context-based access
        AccessDecision.CbacResult cbacResult = checkCbac(securityContext, request);
        decision.setCbacResult(cbacResult);
        if (!cbacResult.isAllowed()) {
            decision.setAllowed(false);
            decision.setDenialReason(AuditLog.AccessDecision.DENIED_CONTEXT);
            decision.setDenialDetails(cbacResult.getReason());
            return decision;
        }

        // 4. Row-level security check
        if (rowLevelSecurityEnabled) {
            AccessDecision.RowLevelResult rowResult = checkRowLevelAccess(securityContext, data, operation);
            decision.setRowLevelResult(rowResult);
            if (!rowResult.isAllowed()) {
                decision.setAllowed(false);
                decision.setDenialReason(AuditLog.AccessDecision.DENIED_ROW_LEVEL);
                decision.setDenialDetails(rowResult.getReason());
                return decision;
            }
        }

        // 5. Column-level security - determine visible columns
        if (columnLevelSecurityEnabled) {
            Set<String> visibleColumns = getVisibleColumns(securityContext, data);
            decision.setVisibleColumns(visibleColumns);
            if (visibleColumns.size() < getAllColumns().size()) {
                decision.setPartialAccess(true);
            }
        }

        decision.setAllowed(true);
        return decision;
    }

    /**
     * RBAC Check - Evaluates role-based access rules
     */
    private AccessDecision.RbacResult checkRbac(SecurityContext context, MyData data, AccessOperation operation) {
        AccessDecision.RbacResult result = new AccessDecision.RbacResult();

        // Admin can do everything
        if (context.hasRole("ADMIN")) {
            result.setAllowed(true);
            result.setMatchedRole("ADMIN");
            return result;
        }

        // Check organization level hierarchy
        switch (data.getOrganizationLevel()) {
            case EXECUTIVE:
                if (!context.isExecutive() && !context.hasRole("EXECUTIVE")) {
                    result.setAllowed(false);
                    result.setReason("Executive level data requires EXECUTIVE role");
                    result.setRequiredRole("EXECUTIVE");
                    return result;
                }
                break;

            case DEPARTMENT:
                if (!context.isExecutive() && !context.isDepartmentHead() 
                    && !context.hasAnyRole("EXECUTIVE", "DEPARTMENT_MANAGER")) {
                    // Must be in same department
                    if (!context.belongsToDepartment(data.getOwnerDepartment())) {
                        result.setAllowed(false);
                        result.setReason("Department level data requires membership in department: " + data.getOwnerDepartment());
                        return result;
                    }
                }
                break;

            case TEAM:
                if (!context.isExecutive() && !context.isDepartmentHead()) {
                    // Must be team lead of the team OR in the same team
                    if (!context.belongsToTeam(data.getOwnerTeam())) {
                        result.setAllowed(false);
                        result.setReason("Team level data requires membership in team: " + data.getOwnerTeam());
                        return result;
                    }
                }
                break;

            case INDIVIDUAL:
                // Only owner, their manager, or executives can access
                if (!context.getUserId().equals(data.getOwnerId())
                    && !context.getUserId().equals(getManagerId(data.getOwnerId()))
                    && !context.isExecutive() && !context.isDepartmentHead()) {
                    result.setAllowed(false);
                    result.setReason("Individual level data can only be accessed by owner or their management chain");
                    return result;
                }
                break;
        }

        // Check operation-specific role requirements
        switch (operation) {
            case DELETE:
                if (!context.hasAnyRole("ADMIN", "DATA_MANAGER") && !context.getUserId().equals(data.getOwnerId())) {
                    result.setAllowed(false);
                    result.setReason("DELETE operation requires ADMIN, DATA_MANAGER role, or data ownership");
                    return result;
                }
                break;

            case UPDATE:
                if (!context.hasAnyRole("ADMIN", "DATA_MANAGER", "EDITOR") 
                    && !context.getUserId().equals(data.getOwnerId())) {
                    result.setAllowed(false);
                    result.setReason("UPDATE operation requires appropriate role or data ownership");
                    return result;
                }
                break;
        }

        result.setAllowed(true);
        result.setMatchedRole(String.join(",", context.getRoles()));
        return result;
    }

    /**
     * ABAC Check - Evaluates attribute-based access rules
     */
    private AccessDecision.AbacResult checkAbac(SecurityContext context, MyData data, AccessOperation operation) {
        AccessDecision.AbacResult result = new AccessDecision.AbacResult();
        Map<String, String> evaluatedAttributes = new HashMap<>();

        // Check sensitivity level vs clearance
        if (data.getSensitivityLevel() != null) {
            UserAttribute.ClearanceLevel requiredClearance = mapSensitivityToClearance(data.getSensitivityLevel());
            evaluatedAttributes.put("requiredClearance", requiredClearance.name());
            evaluatedAttributes.put("userClearance", context.getClearanceLevel() != null ? 
                context.getClearanceLevel().name() : "NONE");

            if (!context.hasClearance(requiredClearance)) {
                result.setAllowed(false);
                result.setReason("Insufficient clearance level. Required: " + requiredClearance + 
                    ", User has: " + context.getClearanceLevel());
                result.setEvaluatedAttributes(evaluatedAttributes);
                return result;
            }
        }

        // Check custom access control rules from database
        List<DataAccessControl> rules = accessControlRepository
            .findActiveRulesForData(data.getId());

        for (DataAccessControl rule : rules) {
            if (!evaluateAccessRule(rule, context, operation)) {
                result.setAllowed(false);
                result.setReason("Access denied by rule: " + rule.getRuleName());
                result.setMatchedRule(rule.getRuleName());
                result.setEvaluatedAttributes(evaluatedAttributes);
                return result;
            }
        }

        result.setAllowed(true);
        result.setEvaluatedAttributes(evaluatedAttributes);
        return result;
    }

    /**
     * CBAC Check - Evaluates context-based access rules
     */
    private AccessDecision.CbacResult checkCbac(SecurityContext context, HttpServletRequest request) {
        AccessDecision.CbacResult result = new AccessDecision.CbacResult();
        Map<String, String> evaluatedContext = new HashMap<>();

        if (request != null) {
            String clientIp = getClientIp(request);
            evaluatedContext.put("clientIp", clientIp);
            evaluatedContext.put("userAgent", request.getHeader("User-Agent"));

            // IP Range check
            if (requireAllowedIp && !isIpAllowed(clientIp)) {
                result.setAllowed(false);
                result.setReason("Access denied: IP address not in allowed ranges");
                result.setEvaluatedContext(evaluatedContext);
                return result;
            }

            // Business hours check
            if (requireBusinessHours && !isWithinBusinessHours()) {
                result.setAllowed(false);
                result.setReason("Access denied: Outside business hours (" + 
                    businessHoursStart + " - " + businessHoursEnd + " " + timezone + ")");
                result.setEvaluatedContext(evaluatedContext);
                return result;
            }
        }

        evaluatedContext.put("businessHours", String.valueOf(isWithinBusinessHours()));
        result.setAllowed(true);
        result.setEvaluatedContext(evaluatedContext);
        return result;
    }

    /**
     * Row-level security check
     */
    private AccessDecision.RowLevelResult checkRowLevelAccess(
            SecurityContext context, MyData data, AccessOperation operation) {
        AccessDecision.RowLevelResult result = new AccessDecision.RowLevelResult();

        // Check specific row-level access rules
        List<DataAccessControl> rowRules = accessControlRepository
            .findRowLevelRules(data.getId(), context.getUserId());

        for (DataAccessControl rule : rowRules) {
            boolean hasPermission = switch (operation) {
                case READ -> rule.getCanRead();
                case CREATE -> rule.getCanCreate();
                case UPDATE -> rule.getCanUpdate();
                case DELETE -> rule.getCanDelete();
            };

            if (rule.getActive() && !hasPermission) {
                result.setAllowed(false);
                result.setReason("Row-level access denied by rule: " + rule.getRuleName());
                return result;
            }
        }

        result.setAllowed(true);
        return result;
    }

    /**
     * Get visible columns based on column-level security
     */
    public Set<String> getVisibleColumns(SecurityContext context, MyData data) {
        Set<String> visibleColumns = new HashSet<>(getAllColumns());

        // Remove columns based on sensitivity and clearance
        if (!context.hasClearance(UserAttribute.ClearanceLevel.CONFIDENTIAL)) {
            visibleColumns.remove("confidentialNotes");
        }

        if (!context.hasClearance(UserAttribute.ClearanceLevel.SECRET)) {
            visibleColumns.remove("financialData");
        }

        // Check column-level access control rules
        List<DataAccessControl> columnRules = accessControlRepository
            .findColumnLevelRules(context.getUserId());

        for (DataAccessControl rule : columnRules) {
            if (rule.getVisibleColumns() != null) {
                try {
                    List<String> allowed = objectMapper.readValue(
                        rule.getVisibleColumns(), new TypeReference<List<String>>() {});
                    visibleColumns.retainAll(allowed);
                } catch (Exception e) {
                    log.warn("Failed to parse visible columns for rule {}", rule.getId(), e);
                }
            }
        }

        return visibleColumns;
    }

    /**
     * Check if access rule matches the context
     */
    private boolean evaluateAccessRule(DataAccessControl rule, SecurityContext context, AccessOperation operation) {
        // Check if rule applies to this principal
        boolean ruleApplies = switch (rule.getPrincipalType()) {
            case USER -> rule.getPrincipalValue().equals(context.getUserId());
            case ROLE -> context.hasRole(rule.getPrincipalValue());
            case DEPARTMENT -> context.belongsToDepartment(rule.getPrincipalValue());
            case TEAM -> context.belongsToTeam(rule.getPrincipalValue());
            case CLEARANCE -> context.hasClearance(
                UserAttribute.ClearanceLevel.valueOf(rule.getPrincipalValue()));
            case ORGANIZATION, ALL -> true;
        };

        if (!ruleApplies) {
            return true; // Rule doesn't apply, so it doesn't block
        }

        // Check attribute conditions
        if (rule.getAttributeConditions() != null) {
            try {
                Map<String, String> conditions = objectMapper.readValue(
                    rule.getAttributeConditions(), new TypeReference<Map<String, String>>() {});
                
                for (Map.Entry<String, String> condition : conditions.entrySet()) {
                    if (!evaluateAttributeCondition(condition.getKey(), condition.getValue(), context)) {
                        return true; // Condition not met, rule doesn't apply
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse attribute conditions for rule {}", rule.getId(), e);
            }
        }

        // Check if operation is allowed
        return switch (operation) {
            case READ -> rule.getCanRead();
            case CREATE -> rule.getCanCreate();
            case UPDATE -> rule.getCanUpdate();
            case DELETE -> rule.getCanDelete();
        };
    }

    private boolean evaluateAttributeCondition(String attribute, String requiredValue, SecurityContext context) {
        return switch (attribute.toLowerCase()) {
            case "department" -> context.belongsToDepartment(requiredValue);
            case "team" -> context.belongsToTeam(requiredValue);
            case "clearance" -> context.hasClearance(UserAttribute.ClearanceLevel.valueOf(requiredValue));
            case "role" -> context.hasRole(requiredValue);
            case "ismanager" -> String.valueOf(context.isManager()).equalsIgnoreCase(requiredValue);
            case "isexecutive" -> String.valueOf(context.isExecutive()).equalsIgnoreCase(requiredValue);
            default -> true;
        };
    }

    private UserAttribute.ClearanceLevel mapSensitivityToClearance(MyData.SensitivityLevel sensitivity) {
        return switch (sensitivity) {
            case PUBLIC -> UserAttribute.ClearanceLevel.PUBLIC;
            case INTERNAL -> UserAttribute.ClearanceLevel.INTERNAL;
            case CONFIDENTIAL -> UserAttribute.ClearanceLevel.CONFIDENTIAL;
            case RESTRICTED -> UserAttribute.ClearanceLevel.SECRET;
        };
    }

    private String getManagerId(String userId) {
        return userAttributeRepository.findByUserId(userId)
            .map(UserAttribute::getManagerId)
            .orElse(null);
    }

    private Set<String> getAllColumns() {
        return Set.of("id", "name", "dataDate", "data", "sensitivityLevel",
            "organizationLevel", "ownerDepartment", "ownerTeam", "ownerId",
            "confidentialNotes", "financialData", "createdAt", "createdBy",
            "updatedAt", "updatedBy");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isIpAllowed(String clientIp) {
        // Simplified IP check - in production, use proper CIDR matching
        for (String range : allowedIpRanges) {
            if (clientIp.startsWith(range.split("/")[0].substring(0, 
                Math.min(range.split("/")[0].length(), clientIp.length())))) {
                return true;
            }
        }
        return true; // Allow all by default for demo
    }

    private boolean isWithinBusinessHours() {
        LocalTime now = LocalTime.now(ZoneId.of(timezone));
        LocalTime start = LocalTime.parse(businessHoursStart);
        LocalTime end = LocalTime.parse(businessHoursEnd);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public enum AccessOperation {
        CREATE, READ, UPDATE, DELETE
    }
}
