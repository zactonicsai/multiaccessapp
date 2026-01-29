package com.enterprise.datasharing.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom JWT converter that extracts roles and authorities from Keycloak tokens.
 * Handles both realm roles and client roles from the JWT token.
 */
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Extract realm roles
        authorities.addAll(extractRealmRoles(jwt));

        // Extract client/resource roles
        authorities.addAll(extractResourceRoles(jwt));

        // Extract custom authorities from claims
        authorities.addAll(extractCustomAuthorities(jwt));

        return authorities;
    }

    /**
     * Extract realm-level roles from Keycloak JWT
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return Collections.emptySet();
        }

        Collection<String> roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
        if (roles == null) {
            return Collections.emptySet();
        }

        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toSet());
    }

    /**
     * Extract client/resource-level roles from Keycloak JWT
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null) {
            return Collections.emptySet();
        }

        Set<GrantedAuthority> authorities = new HashSet<>();

        resourceAccess.forEach((clientId, clientRoles) -> {
            if (clientRoles instanceof Map) {
                Map<String, Object> clientRoleMap = (Map<String, Object>) clientRoles;
                Collection<String> roles = (Collection<String>) clientRoleMap.get(ROLES_CLAIM);
                if (roles != null) {
                    roles.forEach(role -> 
                        authorities.add(new SimpleGrantedAuthority(
                            "ROLE_" + clientId.toUpperCase() + "_" + role.toUpperCase()
                        ))
                    );
                }
            }
        });

        return authorities;
    }

    /**
     * Extract custom authorities based on user attributes in JWT
     */
    private Collection<GrantedAuthority> extractCustomAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Organization level authority
        String orgLevel = jwt.getClaimAsString("organization_level");
        if (orgLevel != null) {
            authorities.add(new SimpleGrantedAuthority("ORG_LEVEL_" + orgLevel.toUpperCase()));
        }

        // Department authority
        String department = jwt.getClaimAsString("department_id");
        if (department != null) {
            authorities.add(new SimpleGrantedAuthority("DEPARTMENT_" + department.toUpperCase()));
        }

        // Team authority
        String team = jwt.getClaimAsString("team_id");
        if (team != null) {
            authorities.add(new SimpleGrantedAuthority("TEAM_" + team.toUpperCase()));
        }

        // Clearance level authority
        String clearance = jwt.getClaimAsString("clearance_level");
        if (clearance != null) {
            authorities.add(new SimpleGrantedAuthority("CLEARANCE_" + clearance.toUpperCase()));
        }

        return authorities;
    }

    /**
     * Utility method to get all roles as strings from JWT
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getAllRoles(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        // Realm roles
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess != null) {
            Collection<String> realmRoles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
            if (realmRoles != null) {
                roles.addAll(realmRoles);
            }
        }

        // Resource roles
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess != null) {
            resourceAccess.forEach((clientId, clientRoles) -> {
                if (clientRoles instanceof Map) {
                    Map<String, Object> clientRoleMap = (Map<String, Object>) clientRoles;
                    Collection<String> clientRoleList = (Collection<String>) clientRoleMap.get(ROLES_CLAIM);
                    if (clientRoleList != null) {
                        roles.addAll(clientRoleList);
                    }
                }
            });
        }

        return roles;
    }
}
