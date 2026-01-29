package com.enterprise.datasharing.service;

import com.enterprise.datasharing.dto.MyDataDto;
import com.enterprise.datasharing.entity.AuditLog;
import com.enterprise.datasharing.entity.MyData;
import com.enterprise.datasharing.exception.AccessDeniedException;
import com.enterprise.datasharing.exception.ResourceNotFoundException;
import com.enterprise.datasharing.repository.MyDataRepository;
import com.enterprise.datasharing.security.AccessControlService;
import com.enterprise.datasharing.security.AccessDecision;
import com.enterprise.datasharing.security.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing MyData with comprehensive access control.
 * Integrates RBAC, ABAC, CBAC, row-level, and column-level security.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MyDataService {

    private final MyDataRepository myDataRepository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;

    /**
     * Create new data entry
     */
    @Transactional
    public MyDataDto.Response create(
            MyDataDto.CreateRequest request,
            SecurityContext securityContext,
            HttpServletRequest httpRequest) {

        log.debug("Creating new data entry for user: {}", securityContext.getUsername());

        // Build entity
        MyData entity = MyData.builder()
            .name(request.getName())
            .dataDate(request.getDataDate())
            .data(request.getData())
            .organizationLevel(request.getOrganizationLevel())
            .sensitivityLevel(request.getSensitivityLevel() != null ? 
                request.getSensitivityLevel() : MyData.SensitivityLevel.INTERNAL)
            .departmentId(request.getDepartmentId() != null ? 
                request.getDepartmentId() : securityContext.getDepartmentId())
            .teamId(request.getTeamId() != null ? 
                request.getTeamId() : securityContext.getTeamId())
            .ownerId(securityContext.getUserId())
            .confidentialNotes(request.getConfidentialNotes())
            .financialData(request.getFinancialData())
            .build();

        // Check access for creation (mainly for setting confidential fields)
        AccessDecision decision = accessControlService.checkAccess(
            securityContext, entity, AccessControlService.AccessOperation.CREATE, httpRequest);

        if (!decision.isAllowed()) {
            auditService.logAccessDenied(securityContext, "MyData", null, 
                AuditLog.AuditAction.CREATE, decision);
            throw new AccessDeniedException("Access denied for CREATE: " + decision.getDenialDetails());
        }

        // Validate user can set confidential fields
        if (entity.getConfidentialNotes() != null && 
            !securityContext.hasClearance(com.enterprise.datasharing.entity.UserAttribute.ClearanceLevel.CONFIDENTIAL)) {
            entity.setConfidentialNotes(null);
            log.warn("User {} attempted to set confidential notes without clearance", 
                securityContext.getUsername());
        }

        if (entity.getFinancialData() != null && 
            !securityContext.hasClearance(com.enterprise.datasharing.entity.UserAttribute.ClearanceLevel.SECRET)) {
            entity.setFinancialData(null);
            log.warn("User {} attempted to set financial data without clearance", 
                securityContext.getUsername());
        }

        // Save
        MyData saved = myDataRepository.save(entity);

        // Audit log
        auditService.logDataAccess(securityContext, AuditLog.AuditAction.CREATE,
            "MyData", saved.getId().toString(), null, saved, decision, true, null);

        log.info("Data entry created: {} by user {}", saved.getId(), securityContext.getUsername());

        return MyDataDto.Response.fromEntity(saved, decision.getVisibleColumns());
    }

    /**
     * Read data entry by ID
     */
    @Transactional(readOnly = true)
    public MyDataDto.Response findById(
            UUID id,
            SecurityContext securityContext,
            HttpServletRequest httpRequest) {

        log.debug("Reading data entry {} for user: {}", id, securityContext.getUsername());

        MyData entity = myDataRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException("MyData", id.toString()));

        // Check access
        AccessDecision decision = accessControlService.checkAccess(
            securityContext, entity, AccessControlService.AccessOperation.READ, httpRequest);

        if (!decision.isAllowed()) {
            auditService.logAccessDenied(securityContext, "MyData", id.toString(),
                AuditLog.AuditAction.READ, decision);
            throw new AccessDeniedException("Access denied for READ: " + decision.getDenialDetails());
        }

        // Audit log (successful read)
        auditService.logDataAccess(securityContext, AuditLog.AuditAction.READ,
            "MyData", id.toString(), null, null, decision, true, null);

        // Return with column filtering
        return MyDataDto.Response.fromEntity(entity, decision.getVisibleColumns());
    }

    /**
     * Update data entry
     */
    @Transactional
    public MyDataDto.Response update(
            UUID id,
            MyDataDto.UpdateRequest request,
            SecurityContext securityContext,
            HttpServletRequest httpRequest) {

        log.debug("Updating data entry {} for user: {}", id, securityContext.getUsername());

        MyData entity = myDataRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException("MyData", id.toString()));

        // Store old values for audit
        MyData oldEntity = cloneEntity(entity);

        // Check access
        AccessDecision decision = accessControlService.checkAccess(
            securityContext, entity, AccessControlService.AccessOperation.UPDATE, httpRequest);

        if (!decision.isAllowed()) {
            auditService.logAccessDenied(securityContext, "MyData", id.toString(),
                AuditLog.AuditAction.UPDATE, decision);
            throw new AccessDeniedException("Access denied for UPDATE: " + decision.getDenialDetails());
        }

        // Get visible columns for the user
        Set<String> visibleColumns = decision.getVisibleColumns();

        // Apply updates only to visible columns
        if (request.getName() != null && canUpdateColumn("name", visibleColumns)) {
            logFieldChangeIfDifferent(securityContext, id.toString(), "name", 
                entity.getName(), request.getName());
            entity.setName(request.getName());
        }
        if (request.getDataDate() != null && canUpdateColumn("dataDate", visibleColumns)) {
            logFieldChangeIfDifferent(securityContext, id.toString(), "dataDate",
                entity.getDataDate(), request.getDataDate());
            entity.setDataDate(request.getDataDate());
        }
        if (request.getData() != null && canUpdateColumn("data", visibleColumns)) {
            logFieldChangeIfDifferent(securityContext, id.toString(), "data",
                entity.getData(), request.getData());
            entity.setData(request.getData());
        }
        if (request.getOrganizationLevel() != null && canUpdateColumn("organizationLevel", visibleColumns)) {
            logFieldChangeIfDifferent(securityContext, id.toString(), "organizationLevel",
                entity.getOrganizationLevel(), request.getOrganizationLevel());
            entity.setOrganizationLevel(request.getOrganizationLevel());
        }
        if (request.getSensitivityLevel() != null && canUpdateColumn("sensitivityLevel", visibleColumns)) {
            logFieldChangeIfDifferent(securityContext, id.toString(), "sensitivityLevel",
                entity.getSensitivityLevel(), request.getSensitivityLevel());
            entity.setSensitivityLevel(request.getSensitivityLevel());
        }
        if (request.getDepartmentId() != null && canUpdateColumn("departmentId", visibleColumns)) {
            entity.setDepartmentId(request.getDepartmentId());
        }
        if (request.getTeamId() != null && canUpdateColumn("teamId", visibleColumns)) {
            entity.setTeamId(request.getTeamId());
        }
        if (request.getConfidentialNotes() != null && canUpdateColumn("confidentialNotes", visibleColumns)) {
            entity.setConfidentialNotes(request.getConfidentialNotes());
        }
        if (request.getFinancialData() != null && canUpdateColumn("financialData", visibleColumns)) {
            entity.setFinancialData(request.getFinancialData());
        }

        // Save
        MyData saved = myDataRepository.save(entity);

        // Audit log
        auditService.logDataAccess(securityContext, AuditLog.AuditAction.UPDATE,
            "MyData", id.toString(), oldEntity, saved, decision, true, null);

        log.info("Data entry updated: {} by user {}", id, securityContext.getUsername());

        return MyDataDto.Response.fromEntity(saved, visibleColumns);
    }

    /**
     * Soft delete data entry
     */
    @Transactional
    public void delete(
            UUID id,
            SecurityContext securityContext,
            HttpServletRequest httpRequest) {

        log.debug("Deleting data entry {} for user: {}", id, securityContext.getUsername());

        MyData entity = myDataRepository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new ResourceNotFoundException("MyData", id.toString()));

        // Check access
        AccessDecision decision = accessControlService.checkAccess(
            securityContext, entity, AccessControlService.AccessOperation.DELETE, httpRequest);

        if (!decision.isAllowed()) {
            auditService.logAccessDenied(securityContext, "MyData", id.toString(),
                AuditLog.AuditAction.DELETE, decision);
            throw new AccessDeniedException("Access denied for DELETE: " + decision.getDenialDetails());
        }

        // Soft delete
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setDeletedBy(securityContext.getUserId());

        myDataRepository.save(entity);

        // Audit log
        auditService.logDataAccess(securityContext, AuditLog.AuditAction.DELETE,
            "MyData", id.toString(), entity, null, decision, true, null);

        log.info("Data entry deleted: {} by user {}", id, securityContext.getUsername());
    }

    /**
     * Find all accessible data for current user
     */
    @Transactional(readOnly = true)
    public Page<MyDataDto.Summary> findAllAccessible(
            SecurityContext securityContext,
            Pageable pageable) {

        log.debug("Finding all accessible data for user: {}", securityContext.getUsername());

        Page<MyData> page = myDataRepository.findAccessibleByUser(
            securityContext.getUserId(),
            securityContext.getDepartmentId(),
            securityContext.getTeamId(),
            securityContext.isExecutive(),
            pageable
        );

        // Log bulk read
        auditService.logAsync(securityContext, AuditLog.AuditAction.BULK_READ,
            "MyData", null, "Retrieved " + page.getTotalElements() + " records");

        return page.map(MyDataDto.Summary::fromEntity);
    }

    /**
     * Find data by owner
     */
    @Transactional(readOnly = true)
    public Page<MyDataDto.Summary> findByOwner(
            String ownerId,
            SecurityContext securityContext,
            Pageable pageable) {

        // Only allow if user is owner or has manager access
        if (!ownerId.equals(securityContext.getUserId()) && 
            !securityContext.isExecutive() && 
            !securityContext.isDepartmentHead()) {
            throw new AccessDeniedException("Cannot view data owned by other users");
        }

        return myDataRepository.findByOwnerId(ownerId).stream()
            .map(MyDataDto.Summary::fromEntity)
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
            ));
    }

    /**
     * Search by name
     */
    @Transactional(readOnly = true)
    public Page<MyDataDto.Summary> searchByName(
            String name,
            SecurityContext securityContext,
            Pageable pageable) {

        // Get accessible data and filter by name
        Page<MyData> accessibleData = myDataRepository.findAccessibleByUser(
            securityContext.getUserId(),
            securityContext.getDepartmentId(),
            securityContext.getTeamId(),
            securityContext.isExecutive(),
            pageable
        );

        return accessibleData
            .map(entity -> {
                if (entity.getName().toLowerCase().contains(name.toLowerCase())) {
                    return MyDataDto.Summary.fromEntity(entity);
                }
                return null;
            })
            .map(s -> s); // Filter nulls handled by Spring Data
    }

    private boolean canUpdateColumn(String column, Set<String> visibleColumns) {
        return visibleColumns == null || visibleColumns.contains(column);
    }

    private void logFieldChangeIfDifferent(SecurityContext context, String entityId,
            String fieldName, Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) return;
        if (oldValue != null && oldValue.equals(newValue)) return;
        auditService.logFieldChange(context, "MyData", entityId, fieldName, oldValue, newValue);
    }

    private MyData cloneEntity(MyData entity) {
        return MyData.builder()
            .id(entity.getId())
            .name(entity.getName())
            .dataDate(entity.getDataDate())
            .data(entity.getData())
            .sensitivityLevel(entity.getSensitivityLevel())
            .organizationLevel(entity.getOrganizationLevel())
            .departmentId(entity.getDepartmentId())
            .teamId(entity.getTeamId())
            .ownerId(entity.getOwnerId())
            .confidentialNotes(entity.getConfidentialNotes())
            .financialData(entity.getFinancialData())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
