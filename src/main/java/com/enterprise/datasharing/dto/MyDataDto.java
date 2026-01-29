package com.enterprise.datasharing.dto;

import com.enterprise.datasharing.entity.MyData;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Objects for MyData entity
 */
public class MyDataDto {

    /**
     * Request DTO for creating new data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be less than 255 characters")
        private String name;

        @NotNull(message = "Data date is required")
        private LocalDate dataDate;

        @Size(max = 10000, message = "Data must be less than 10000 characters")
        private String data;

        @NotNull(message = "Organization level is required")
        private MyData.OrganizationLevel organizationLevel;

        private MyData.SensitivityLevel sensitivityLevel;

        private String departmentId;

        private String teamId;

        // Optional confidential fields (require appropriate clearance)
        private String confidentialNotes;

        private String financialData;
    }

    /**
     * Request DTO for updating existing data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        @Size(max = 255, message = "Name must be less than 255 characters")
        private String name;

        private LocalDate dataDate;

        @Size(max = 10000, message = "Data must be less than 10000 characters")
        private String data;

        private MyData.OrganizationLevel organizationLevel;

        private MyData.SensitivityLevel sensitivityLevel;

        private String departmentId;

        private String teamId;

        private String confidentialNotes;

        private String financialData;
    }

    /**
     * Response DTO with column-level filtering
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID id;
        private String name;
        private LocalDate dataDate;
        private String data;
        private MyData.SensitivityLevel sensitivityLevel;
        private MyData.OrganizationLevel organizationLevel;
        private String departmentId;
        private String teamId;
        private String ownerId;
        
        // Sensitive fields - may be null based on clearance
        private String confidentialNotes;
        private String financialData;

        // Audit info
        private LocalDateTime createdAt;
        private String createdBy;
        private LocalDateTime updatedAt;
        private String updatedBy;

        // Access info
        private Set<String> visibleColumns;
        private boolean partialAccess;

        /**
         * Create response from entity with column filtering
         */
        public static Response fromEntity(MyData entity, Set<String> visibleColumns) {
            ResponseBuilder builder = Response.builder()
                .id(entity.getId())
                .visibleColumns(visibleColumns)
                .partialAccess(visibleColumns != null && visibleColumns.size() < 15);

            // Only include visible columns
            if (visibleColumns == null || visibleColumns.contains("name")) {
                builder.name(entity.getName());
            }
            if (visibleColumns == null || visibleColumns.contains("dataDate")) {
                builder.dataDate(entity.getDataDate());
            }
            if (visibleColumns == null || visibleColumns.contains("data")) {
                builder.data(entity.getData());
            }
            if (visibleColumns == null || visibleColumns.contains("sensitivityLevel")) {
                builder.sensitivityLevel(entity.getSensitivityLevel());
            }
            if (visibleColumns == null || visibleColumns.contains("organizationLevel")) {
                builder.organizationLevel(entity.getOrganizationLevel());
            }
            if (visibleColumns == null || visibleColumns.contains("departmentId")) {
                builder.departmentId(entity.getDepartmentId());
            }
            if (visibleColumns == null || visibleColumns.contains("teamId")) {
                builder.teamId(entity.getTeamId());
            }
            if (visibleColumns == null || visibleColumns.contains("ownerId")) {
                builder.ownerId(entity.getOwnerId());
            }
            if (visibleColumns == null || visibleColumns.contains("confidentialNotes")) {
                builder.confidentialNotes(entity.getConfidentialNotes());
            }
            if (visibleColumns == null || visibleColumns.contains("financialData")) {
                builder.financialData(entity.getFinancialData());
            }
            if (visibleColumns == null || visibleColumns.contains("createdAt")) {
                builder.createdAt(entity.getCreatedAt());
            }
            if (visibleColumns == null || visibleColumns.contains("createdBy")) {
                builder.createdBy(entity.getCreatedBy());
            }
            if (visibleColumns == null || visibleColumns.contains("updatedAt")) {
                builder.updatedAt(entity.getUpdatedAt());
            }
            if (visibleColumns == null || visibleColumns.contains("updatedBy")) {
                builder.updatedBy(entity.getUpdatedBy());
            }

            return builder.build();
        }

        /**
         * Create full response from entity (no filtering)
         */
        public static Response fromEntity(MyData entity) {
            return fromEntity(entity, null);
        }
    }

    /**
     * Summary DTO for list views
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private UUID id;
        private String name;
        private LocalDate dataDate;
        private MyData.SensitivityLevel sensitivityLevel;
        private MyData.OrganizationLevel organizationLevel;
        private String departmentId;
        private String teamId;
        private LocalDateTime createdAt;

        public static Summary fromEntity(MyData entity) {
            return Summary.builder()
                .id(entity.getId())
                .name(entity.getName())
                .dataDate(entity.getDataDate())
                .sensitivityLevel(entity.getSensitivityLevel())
                .organizationLevel(entity.getOrganizationLevel())
                .departmentId(entity.getDepartmentId())
                .teamId(entity.getTeamId())
                .createdAt(entity.getCreatedAt())
                .build();
        }
    }
}
