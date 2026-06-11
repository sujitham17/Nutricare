package com.nutricare.nutricarebackend.mongo.document;

import com.nutricare.nutricarebackend.entity.MealComplianceStatus;
import com.nutricare.nutricarebackend.entity.MealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "meal_compliance_logs")
@CompoundIndex(name = "idx_compliance_user_submitted", def = "{'userId': 1, 'submittedAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealComplianceLogDocument {

    @Id
    private String id;

    @Indexed
    private Long mysqlComplianceId;

    @Indexed
    private Long userId;

    @Indexed
    private Long dieticianId;

    @Indexed
    private String weeklyMealPlanId;

    private LocalDate mealDate;
    private MealType mealType;
    private MealComplianceStatus status;
    private String reason;
    private LocalDateTime submittedAt;
}
