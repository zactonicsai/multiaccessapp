package com.enterprise.datasharing.controller;

import com.enterprise.datasharing.dto.MyDataDto;
import com.enterprise.datasharing.security.SecurityContext;
import com.enterprise.datasharing.service.MyDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for MyData CRUD operations.
 * All operations are subject to RBAC, ABAC, and CBAC access controls.
 * All activities are audited.
 */
@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Management", description = "CRUD operations for data with access control")
@SecurityRequirement(name = "bearerAuth")
public class MyDataController {

    private final MyDataService myDataService;

    /**
     * Create new data entry
     */
    @PostMapping
    @Operation(summary = "Create new data entry",
        description = "Creates a new data entry. Access level and visibility determined by organization level.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Data created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyDataDto.Response> create(
            @Valid @RequestBody MyDataDto.CreateRequest request,
            HttpServletRequest httpRequest) {

        SecurityContext context = SecurityContext.fromCurrentContext();
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Create request from user: {}", context.getUsername());

        MyDataDto.Response response = myDataService.create(request, context, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get data entry by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get data entry by ID",
        description = "Retrieves a data entry. Columns visible depend on user clearance level.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Data not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyDataDto.Response> findById(
            @Parameter(description = "Data entry ID") @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        SecurityContext context = SecurityContext.fromCurrentContext();
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("Read request for {} from user: {}", id, context.getUsername());

        MyDataDto.Response response = myDataService.findById(id, context, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Update data entry
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update data entry",
        description = "Updates a data entry. Only visible columns can be updated.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Data not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyDataDto.Response> update(
            @Parameter(description = "Data entry ID") @PathVariable UUID id,
            @Valid @RequestBody MyDataDto.UpdateRequest request,
            HttpServletRequest httpRequest) {

        SecurityContext context = SecurityContext.fromCurrentContext();
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Update request for {} from user: {}", id, context.getUsername());

        MyDataDto.Response response = myDataService.update(id, request, context, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Partial update data entry
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Partial update data entry",
        description = "Partially updates a data entry. Only provided fields are updated.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Data not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyDataDto.Response> partialUpdate(
            @Parameter(description = "Data entry ID") @PathVariable UUID id,
            @RequestBody MyDataDto.UpdateRequest request,
            HttpServletRequest httpRequest) {

        SecurityContext context = SecurityContext.fromCurrentContext();
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Partial update request for {} from user: {}", id, context.getUsername());

        MyDataDto.Response response = myDataService.update(id, request, context, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete data entry (soft delete)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete data entry",
        description = "Soft deletes a data entry. Requires owner permission or ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Data deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Data not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Data entry ID") @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        SecurityContext context = SecurityContext.fromCurrentContext();
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Delete request for {} from user: {}", id, context.getUsername());

        myDataService.delete(id, context, httpRequest);
        return ResponseEntity.noContent().build();
    }

    /**
     * List all accessible data
     */
    @GetMapping
    @Operation(summary = "List all accessible data",
        description = "Lists all data entries accessible to the current user based on organization hierarchy.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data retrieved successfully")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MyDataDto.Summary>> findAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        SecurityContext context = SecurityContext.fromCurrentContext();
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("List request from user: {}", context.getUsername());

        Page<MyDataDto.Summary> response = myDataService.findAllAccessible(context, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get my data (owned by current user)
     */
    @GetMapping("/my")
    @Operation(summary = "Get my data",
        description = "Lists all data entries owned by the current user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data retrieved successfully")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MyDataDto.Summary>> findMyData(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        SecurityContext context = SecurityContext.fromCurrentContext();
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("My data request from user: {}", context.getUsername());

        Page<MyDataDto.Summary> response = myDataService.findByOwner(
            context.getUserId(), context, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Search by name
     */
    @GetMapping("/search")
    @Operation(summary = "Search data by name",
        description = "Searches data entries by name within accessible data.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MyDataDto.Summary>> search(
            @Parameter(description = "Search query") @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        SecurityContext context = SecurityContext.fromCurrentContext();
        if (context == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("Search request for '{}' from user: {}", q, context.getUsername());

        Page<MyDataDto.Summary> response = myDataService.searchByName(q, context, pageable);
        return ResponseEntity.ok(response);
    }
}
