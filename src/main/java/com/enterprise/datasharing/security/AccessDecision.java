package com.enterprise.datasharing.security;

import com.enterprise.datasharing.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * Represents the result of an access control decision.
 * Contains detailed information about why access was granted or denied.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessDecision {

    private String userId;
    private String entityId;
    private AccessControlService.AccessOperation operation;
    
    private boolean allowed;
    private boolean partialAccess;
    
    private AuditLog.AccessDecision denialReason;
    private String denialDetails;

    private RbacResult rbacResult;
    private AbacResult abacResult;
    private CbacResult cbacResult;
    private RowLevelResult rowLevelResult;
    
    private Set<String> visibleColumns;

    /**
     * RBAC evaluation result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RbacResult {
        private boolean allowed;
        private String matchedRole;
        private String requiredRole;
        private String reason;
    }

    /**
     * ABAC evaluation result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AbacResult {
        private boolean allowed;
        private String matchedRule;
        private Map<String, String> evaluatedAttributes;
        private String reason;
    }

    /**
     * CBAC evaluation result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CbacResult {
        private boolean allowed;
        private Map<String, String> evaluatedContext;
        private String reason;
    }

    /**
     * Row-level security result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowLevelResult {
        private boolean allowed;
        private String matchedRule;
        private String reason;
    }

    /**
     * Build summary for audit logging
     */
    public String buildAuditSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Access Decision: ").append(allowed ? "GRANTED" : "DENIED");
        
        if (!allowed && denialReason != null) {
            sb.append(" | Reason: ").append(denialReason);
            if (denialDetails != null) {
                sb.append(" - ").append(denialDetails);
            }
        }
        
        if (partialAccess) {
            sb.append(" | Partial Access: columns filtered");
        }
        
        return sb.toString();
    }
}
