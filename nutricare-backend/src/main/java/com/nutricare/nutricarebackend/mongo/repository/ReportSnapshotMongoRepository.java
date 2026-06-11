package com.nutricare.nutricarebackend.mongo.repository;

import com.nutricare.nutricarebackend.mongo.document.ReportSnapshotDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReportSnapshotMongoRepository extends MongoRepository<ReportSnapshotDocument, String> {

    List<ReportSnapshotDocument> findAllByOrderByCreatedAtDesc();

    List<ReportSnapshotDocument> findByGeneratedByOrderByCreatedAtDesc(Long generatedBy);
}
