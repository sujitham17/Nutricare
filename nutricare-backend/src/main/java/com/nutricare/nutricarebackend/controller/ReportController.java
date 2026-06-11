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
public class ReportController {

    private final ReportService reportService;
    private final ExcelExportService excelExportService;

    @GetMapping("/api/reports/my-summary")
    public ResponseEntity<ReportSummaryResponse> getMySummary(Authentication authentication) {
        return ResponseEntity.ok(reportService.getMySummary(authentication.getName()));
    }

    @GetMapping("/api/reports/my-summary/excel")
    public ResponseEntity<byte[]> exportMySummary(Authentication authentication) {
        return excelResponse(excelExportService.userReport(authentication.getName()), "nutricare-user-report.xlsx");
    }

    @GetMapping("/api/reports/dietician/excel")
    public ResponseEntity<byte[]> exportDieticianSummary(Authentication authentication) {
        return excelResponse(excelExportService.dieticianReport(authentication.getName()), "nutricare-dietician-report.xlsx");
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
        return excelResponse(excelExportService.dieticianUserReport(authentication.getName(), userId), "nutricare-dietician-user-report.xlsx");
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
