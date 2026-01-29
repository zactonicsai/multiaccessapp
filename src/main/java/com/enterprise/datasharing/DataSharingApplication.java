package com.enterprise.datasharing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enterprise Data Sharing Application
 * 
 * Demonstrates multi-level data sharing with comprehensive access control:
 * - RBAC (Role-Based Access Control): Executive, Department Manager, Team Lead, Individual
 * - ABAC (Attribute-Based Access Control): Department, Team, Clearance Level
 * - CBAC (Context-Based Access Control): Time, IP, Location
 * 
 * Features:
 * - Keycloak OAuth2/OIDC Authentication
 * - Row-level and Column-level security
 * - Comprehensive audit logging
 * - Multi-tenant data isolation
 */
@SpringBootApplication
@EnableJpaAuditing
public class DataSharingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataSharingApplication.class, args);
    }
}
