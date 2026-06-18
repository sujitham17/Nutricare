package com.nutricare.nutricarebackend.controller;

import com.nutricare.nutricarebackend.dto.ReportSnapshotRequest;
import com.nutricare.nutricarebackend.dto.ReportSummaryResponse;
import com.nutricare.nutricarebackend.mongo.document.ReportSnapshotDocument;
import com.nutricare.nutricarebackend.service.ExcelExportService;
import com.nutricare.nutricarebackend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ReportController {

    private final ReportService reportService;
    private final ExcelExportService excelExportService;
    private final com.nutricare.nutricarebackend.repository.UserRepository userRepository;

    @GetMapping("/api/reports/my-summary")
    public ResponseEntity<ReportSummaryResponse> getMySummary(Authentication authentication) {
        return ResponseEntity.ok(reportService.getMySummary(authentication.getName()));
    }

    @GetMapping("/api/reports/my-summary/excel")
    public ResponseEntity<byte[]> exportMySummary(Authentication authentication) {
        log.info("exportMySummary endpoint called for: {}", authentication.getName());
        try {
            com.nutricare.nutricarebackend.entity.User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
            log.info("exportMySummary authenticated user id: {}, email: {}, role: {}, export type: my-summary-excel",
                    user.getId(), user.getEmail(), user.getRole().name());
            byte[] content = excelExportService.userReport(authentication.getName());
            return excelResponse(content, "nutricare-user-report.xlsx");
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            log.error("ResponseStatusException in exportMySummary: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Throwable ex) {
            log.error("Throwable in exportMySummary: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @GetMapping("/api/reports/dietician/excel")
    public ResponseEntity<byte[]> exportDieticianSummary(Authentication authentication) {
        log.info("exportDieticianSummary endpoint called for: {}", authentication.getName());
        try {
            com.nutricare.nutricarebackend.entity.User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
            log.info("exportDieticianSummary authenticated user id: {}, email: {}, role: {}, export type: dietician-summary-excel",
                    user.getId(), user.getEmail(), user.getRole().name());
            byte[] content = excelExportService.dieticianReport(authentication.getName());
            return excelResponse(content, "nutricare-dietician-report.xlsx");
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            log.error("ResponseStatusException in exportDieticianSummary: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Throwable ex) {
            log.error("Throwable in exportDieticianSummary: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @GetMapping("/api/reports/user/{userId}")
    public ResponseEntity<ReportSummaryResponse> getUserSummary(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(reportService.getUserSummary(authentication.getName(), userId));
    }

    @GetMapping("/api/reports/user/{userId}/excel")
    public ResponseEntity<byte[]> exportUserSummary(
            Authentication authentication,
            @PathVariable Long userId
    ) {
        log.info("exportUserSummary endpoint called for: {} targeting userId: {}", authentication.getName(), userId);
        try {
            com.nutricare.nutricarebackend.entity.User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
            log.info("exportUserSummary authenticated user id: {}, email: {}, role: {}, export type: dietician-user-report-excel",
                    user.getId(), user.getEmail(), user.getRole().name());
            byte[] content = excelExportService.dieticianUserReport(authentication.getName(), userId);
            return excelResponse(content, "nutricare-dietician-user-report.xlsx");
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            log.error("ResponseStatusException in exportUserSummary: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Throwable ex) {
            log.error("Throwable in exportUserSummary: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    @PostMapping("/api/reports/snapshots")
    public ResponseEntity<ReportSnapshotDocument> saveSnapshot(
            Authentication authentication,
            @Valid @RequestBody ReportSnapshotRequest request
    ) {
        return ResponseEntity.ok(reportService.saveSnapshot(authentication.getName(), request));
    }

    private ResponseEntity<byte[]> excelResponse(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
