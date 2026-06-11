package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.AdminDashboardSummaryResponse;
import com.nutricare.nutricarebackend.dto.AdminClientProgramResponse;
import com.nutricare.nutricarebackend.dto.AdminCreateDieticianRequest;
import com.nutricare.nutricarebackend.dto.AdminCreateUserRequest;
import com.nutricare.nutricarebackend.dto.AdminPaymentResponse;
import com.nutricare.nutricarebackend.dto.AdminRevenueResponse;
import com.nutricare.nutricarebackend.dto.AdminSubscriptionResponse;
import com.nutricare.nutricarebackend.dto.AdminUserActionRequest;
import com.nutricare.nutricarebackend.dto.AdminUserResponse;
import com.nutricare.nutricarebackend.dto.AppointmentResponse;
import com.nutricare.nutricarebackend.dto.NotificationLogResponse;
import com.nutricare.nutricarebackend.dto.SystemStatsResponse;
import com.nutricare.nutricarebackend.dto.DiseaseRequest;
import com.nutricare.nutricarebackend.dto.DiseaseResponse;
import com.nutricare.nutricarebackend.mongo.document.AuditLogDocument;
import com.nutricare.nutricarebackend.mongo.document.ReportSnapshotDocument;
import com.nutricare.nutricarebackend.service.AdminService;
import com.nutricare.nutricarebackend.service.ExcelExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ExcelExportService excelExportService;

    @GetMapping("/dashboard-summary")
    public ResponseEntity<AdminDashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(adminService.getDashboardSummary());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> createUser(
            Authentication authentication,
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminService.createUser(authentication.getName(), request));
    }

    @PutMapping("/users/{id}/approve")
    public ResponseEntity<AdminUserResponse> approveUser(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveUser(authentication.getName(), id));
    }

    @PatchMapping("/users/{id}/approve")
    public ResponseEntity<AdminUserResponse> patchApproveUser(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveUser(authentication.getName(), id));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<AdminUserResponse> suspendUser(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminUserActionRequest request
    ) {
        return ResponseEntity.ok(adminService.suspendUser(authentication.getName(), id, request));
    }

    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<AdminUserResponse> patchSuspendUser(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminUserActionRequest request
    ) {
        return ResponseEntity.ok(adminService.suspendUser(authentication.getName(), id, request));
    }

    @PutMapping("/users/{id}/reject")
    public ResponseEntity<AdminUserResponse> rejectUser(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminUserActionRequest request
    ) {
        return ResponseEntity.ok(adminService.rejectUser(authentication.getName(), id, request));
    }

    @PatchMapping("/users/{id}/reject")
    public ResponseEntity<AdminUserResponse> patchRejectUser(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminUserActionRequest request
    ) {
        return ResponseEntity.ok(adminService.rejectUser(authentication.getName(), id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminUserActionRequest request
    ) {
        adminService.deleteUser(authentication.getName(), id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/dieticians")
    public ResponseEntity<List<AdminUserResponse>> getDieticians() {
        return ResponseEntity.ok(adminService.getDieticians());
    }

    @PostMapping("/dieticians")
    public ResponseEntity<AdminUserResponse> createDietician(
            Authentication authentication,
            @Valid @RequestBody AdminCreateDieticianRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminService.createDietician(authentication.getName(), request));
    }

    @PatchMapping("/dieticians/{id}/approve")
    public ResponseEntity<AdminUserResponse> approveDietician(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveDietician(authentication.getName(), id));
    }

    @PatchMapping("/dieticians/{id}/suspend")
    public ResponseEntity<AdminUserResponse> suspendDietician(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminUserActionRequest request
    ) {
        return ResponseEntity.ok(adminService.suspendDietician(authentication.getName(), id, request));
    }

    @PatchMapping("/dieticians/{id}/reject")
    public ResponseEntity<AdminUserResponse> rejectDietician(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminUserActionRequest request
    ) {
        return ResponseEntity.ok(adminService.rejectDietician(authentication.getName(), id, request));
    }

    @DeleteMapping("/dieticians/{id}")
    public ResponseEntity<Void> deleteDietician(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminUserActionRequest request
    ) {
        adminService.deleteDietician(authentication.getName(), id, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<AdminSubscriptionResponse>> getSubscriptions() {
        return ResponseEntity.ok(adminService.getSubscriptions());
    }

    @PutMapping("/subscriptions/{id}/deactivate")
    public ResponseEntity<AdminSubscriptionResponse> deactivateSubscription(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(adminService.deactivateSubscription(authentication.getName(), id));
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Void> deleteSubscription(Authentication authentication, @PathVariable Long id) {
        adminService.deleteSubscription(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> getAppointments() {
        return ResponseEntity.ok(adminService.getAppointments());
    }

    @GetMapping("/meetings")
    public ResponseEntity<List<AppointmentResponse>> getMeetings() {
        return ResponseEntity.ok(adminService.getMeetings());
    }

    @GetMapping("/client-programs")
    public ResponseEntity<List<AdminClientProgramResponse>> getClientPrograms() {
        return ResponseEntity.ok(adminService.getClientPrograms());
    }

    @GetMapping("/payments")
    public ResponseEntity<List<AdminPaymentResponse>> getPayments() {
        return ResponseEntity.ok(adminService.getPayments());
    }

    @GetMapping("/notification-logs")
    public ResponseEntity<List<NotificationLogResponse>> getNotificationLogs(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String channel,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long receiverId
    ) {
        return ResponseEntity.ok(adminService.getNotificationLogs(receiverId, channel, status));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogDocument>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

    @GetMapping("/report-snapshots")
    public ResponseEntity<List<ReportSnapshotDocument>> getReportSnapshots() {
        return ResponseEntity.ok(adminService.getReportSnapshots());
    }

    @GetMapping("/revenue")
    public ResponseEntity<AdminRevenueResponse> getRevenue() {
        return ResponseEntity.ok(adminService.getRevenue());
    }

    @GetMapping("/reports/excel")
    public ResponseEntity<byte[]> exportAdminReport() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nutricare-admin-report.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelExportService.adminReport());
    }

    @GetMapping("/system-stats")
    public ResponseEntity<SystemStatsResponse> getSystemStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }

    @PostMapping("/diseases")
    public ResponseEntity<DiseaseResponse> createDisease(
            Authentication authentication,
            @Valid @RequestBody DiseaseRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminService.createDisease(authentication.getName(), request));
    }

    @PutMapping("/diseases/{id}")
    public ResponseEntity<DiseaseResponse> updateDisease(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody DiseaseRequest request
    ) {
        return ResponseEntity.ok(adminService.updateDisease(authentication.getName(), id, request));
    }

    @DeleteMapping("/diseases/{id}")
    public ResponseEntity<Void> deleteDisease(
            Authentication authentication,
            @PathVariable Long id
    ) {
        adminService.deleteDisease(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
