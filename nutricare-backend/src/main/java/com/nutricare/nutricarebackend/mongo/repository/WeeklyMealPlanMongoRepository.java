package com.nutricare.nutricarebackend.mongo.repository;

import com.nutricare.nutricarebackend.mongo.document.WeeklyMealPlanDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyMealPlanMongoRepository extends MongoRepository<WeeklyMealPlanDocument, String> {

    Optional<WeeklyMealPlanDocument> findByDietPlanId(Long dietPlanId);

    List<WeeklyMealPlanDocument> findByMysqlUserIdOrderByCreatedAtDesc(Long mysqlUserId);

    List<WeeklyMealPlanDocument> findByMysqlDieticianIdOrderByCreatedAtDesc(Long mysqlDieticianId);
}
