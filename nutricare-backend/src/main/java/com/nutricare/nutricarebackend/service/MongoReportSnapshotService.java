package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.mongo.document.ReportSnapshotDocument;
import com.nutricare.nutricarebackend.mongo.repository.ReportSnapshotMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MongoReportSnapshotService {

    private final ReportSnapshotMongoRepository reportSnapshotMongoRepository;

    public ReportSnapshotDocument save(
            String reportType,
            Long userId,
            Long dieticianId,
            Long generatedBy,
            Role generatedByRole,
            Object data
    ) {
        return reportSnapshotMongoRepository.save(ReportSnapshotDocument.builder()
                .reportType(reportType)
                .userId(userId)
                .dieticianId(dieticianId)
                .generatedBy(generatedBy)
                .generatedByRole(generatedByRole)
                .data(data)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public List<ReportSnapshotDocument> findAll() {
        return reportSnapshotMongoRepository.findAllByOrderByCreatedAtDesc();
    }
}
