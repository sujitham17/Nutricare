package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.mongo.document.AuditLogDocument;
import com.nutricare.nutricarebackend.mongo.repository.AuditLogMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMongoRepository auditLogMongoRepository;

    public AuditLogDocument record(Long actorId, Role actorRole, String action, String module, String description) {
        return auditLogMongoRepository.save(AuditLogDocument.builder()
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .module(module)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public List<AuditLogDocument> findAll() {
        return auditLogMongoRepository.findAllByOrderByCreatedAtDesc();
    }

    public void record(
            Long actorUserId,
            Long userId,
            Long dieticianId,
            Long appointmentId,
            Long targetUserId,
            String action,
            String entityType,
            String entityId,
            String details
    ) {
        record(actorUserId, null, action, entityType, details);
    }
}
