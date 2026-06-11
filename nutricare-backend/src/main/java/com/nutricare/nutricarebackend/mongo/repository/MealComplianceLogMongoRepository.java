package com.nutricare.nutricarebackend.mongo.repository;

import com.nutricare.nutricarebackend.mongo.document.MealComplianceLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MealComplianceLogMongoRepository extends MongoRepository<MealComplianceLogDocument, String> {

    List<MealComplianceLogDocument> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<MealComplianceLogDocument> findByDieticianIdOrderBySubmittedAtDesc(Long dieticianId);
}
