package com.nutricare.nutricarebackend.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "weekly_meal_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyMealPlanDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long dietPlanId;

    @Indexed
    private Long mysqlUserId;

    @Indexed
    private Long mysqlDieticianId;

    private String title;
    private String description;
    private String programGoal;
    private String waterIntake;
    private Integer calories;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<WeeklyMealDay> days;
    private List<WeeklyMealDetail> mealDetails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyMealDay {
        private LocalDate date;
        private String breakfast;
        private String lunch;
        private String dinner;
        private String snacks;
        private String instructions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyMealDetail {
        private Long mysqlMealId;
        private Integer dayNumber;
        private com.nutricare.nutricarebackend.entity.MealType mealType;
        private String mealName;
        private String mealTime;
        private String waterIntake;
        private String instructions;
    }
}
