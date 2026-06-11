package com.nutricare.nutricarebackend.mongo.repository;

import com.nutricare.nutricarebackend.mongo.document.AuditLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogMongoRepository extends MongoRepository<AuditLogDocument, String> {

    List<AuditLogDocument> findAllByOrderByCreatedAtDesc();

    List<AuditLogDocument> findByActorIdOrderByCreatedAtDesc(Long actorId);
}
