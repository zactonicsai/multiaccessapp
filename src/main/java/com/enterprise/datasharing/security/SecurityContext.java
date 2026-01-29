package com.enterprise.datasharing.security;

import com.enterprise.datasharing.entity.UserAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;

/**
 * Represents the security context of the current user.
 * Contains all information needed for RBAC, ABAC, and CBAC decisions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityContext {

    private String userId;
    private String username;
    private String email;
    private Set<String> roles;
    private String department;
    private String team;
    private UserAttribute.OrganizationLevel organizationLevel;
    private UserAttribute.ClearanceLevel clearanceLevel;
    private String managerId;
    private boolean isManager;
    private boolean isDepartmentHead;
    private boolean isExecutive;

    // Request context for CBAC
    private String ipAddress;
    private String userAgent;
    private String requestUri;

    /**
     * Create SecurityContext from the current Spring Security context
     */
    public static SecurityContext fromCurrentContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            return null;
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();

        return SecurityContext.builder()
            .userId(jwt.getSubject())
            .username(jwt.getClaimAsString("preferred_username"))
            .email(jwt.getClaimAsString("email"))
            .roles(CustomJwtAuthenticationConverter.getAllRoles(jwt))
            .department(jwt.getClaimAsString("department"))
            .team(jwt.getClaimAsString("team"))
            .organizationLevel(parseOrgLevel(jwt.getClaimAsString("organization_level")))
            .clearanceLevel(parseClearanceLevel(jwt.getClaimAsString("clearance_level")))
            .managerId(jwt.getClaimAsString("manager_id"))
            .isManager(getBooleanClaim(jwt, "is_manager"))
            .isDepartmentHead(getBooleanClaim(jwt, "is_department_head"))
            .isExecutive(getBooleanClaim(jwt, "is_executive"))
            .build();
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... checkRoles) {
        if (roles == null) return false;
        for (String role : checkRoles) {
            if (roles.contains(role)) return true;
        }
        return false;
    }

    /**
     * Check if user belongs to a department
     */
    public boolean belongsToDepartment(String dept) {
        return department != null && department.equals(dept);
    }

    /**
     * Check if user belongs to a team
     */
    public boolean belongsToTeam(String t) {
        return team != null && team.equals(t);
    }

    /**
     * Check if user has required clearance level
     */
    public boolean hasClearance(UserAttribute.ClearanceLevel requiredLevel) {
        if (clearanceLevel == null) return false;
        return clearanceLevel.ordinal() >= requiredLevel.ordinal();
    }

    /**
     * Check if user can access data at given organization level
     */
    public boolean canAccessOrgLevel(UserAttribute.OrganizationLevel dataOrgLevel) {
        if (organizationLevel == null) return false;
        // Executives can access everything, individuals can only access individual level
        return organizationLevel.ordinal() <= dataOrgLevel.ordinal();
    }

    private static UserAttribute.OrganizationLevel parseOrgLevel(String value) {
        if (value == null) return UserAttribute.OrganizationLevel.INDIVIDUAL;
        try {
            return UserAttribute.OrganizationLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserAttribute.OrganizationLevel.INDIVIDUAL;
        }
    }

    private static UserAttribute.ClearanceLevel parseClearanceLevel(String value) {
        if (value == null) return UserAttribute.ClearanceLevel.PUBLIC;
        try {
            return UserAttribute.ClearanceLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserAttribute.ClearanceLevel.PUBLIC;
        }
    }

    private static boolean getBooleanClaim(Jwt jwt, String claim) {
        Boolean value = jwt.getClaim(claim);
        return value != null && value;
    }
}
