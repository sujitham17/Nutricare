package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.DietPlanRequest;
import com.nutricare.nutricarebackend.dto.DietPlanMealResponse;
import com.nutricare.nutricarebackend.dto.DietPlanResponse;
import com.nutricare.nutricarebackend.dto.WeeklyMealRequest;
import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.DietPlanMeal;
import com.nutricare.nutricarebackend.entity.MealType;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.mongo.document.WeeklyMealPlanDocument;
import com.nutricare.nutricarebackend.repository.DietPlanMealRepository;
import com.nutricare.nutricarebackend.repository.DietPlanRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DietPlanService {

    private final DietPlanRepository dietPlanRepository;
    private final DietPlanMealRepository dietPlanMealRepository;
    private final UserRepository userRepository;
    private final MealComplianceService mealComplianceService;
    private final AccessControlService accessControlService;
    private final NotificationService notificationService;
    private final MongoWeeklyMealPlanService mongoWeeklyMealPlanService;
    private final AuditLogService auditLogService;

    @Transactional
    public DietPlanResponse createDietPlan(String email, DietPlanRequest request) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can create diet plans");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diet plan can only be assigned to a user");
        }
        accessControlService.requireAssignedDietician(dietician, user);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
        if (ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weekly meal plan duration must be exactly 7 days");
        }
        validateWeeklyMeals(request.getMeals());

        DietPlan dietPlan = DietPlan.builder()
                .user(user)
                .dietician(dietician)
                .title(request.getTitle())
                .description(request.getDescription())
                .programGoal(request.getProgramGoal())
                .breakfast(request.getBreakfast())
                .lunch(request.getLunch())
                .dinner(request.getDinner())
                .snacks(request.getSnacks())
                .waterIntake(request.getWaterIntake())
                .calories(request.getCalories())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        DietPlan saved = dietPlanRepository.save(dietPlan);
        mealComplianceService.createWeeklyMealsAndCompliance(saved, request.getMeals());
        WeeklyMealPlanDocument weeklyDocument = saveWeeklyMealPlanDocument(saved);
        saved.setMongoDocumentId(weeklyDocument.getId());
        saved = dietPlanRepository.save(saved);
        notificationService.sendNotification(
                user,
                dietician,
                "Weekly Meal Plan Assigned",
                "Your weekly meal plan has been assigned by " + dietician.getFullName() + ".",
                "WEEKLY_MEAL_PLAN_ASSIGNED"
        );

        return toResponse(saved);
    }

    public List<DietPlanResponse> getMyDietPlans(String email) {
        User user = getAuthenticatedUser(email);
        requireRole(user, Role.USER, "Only users can view their diet plans");

        return dietPlanRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DietPlanResponse> getUserDietPlans(String email, Long userId) {
        User dietician = getAuthenticatedUser(email);
        if (dietician.getRole() != Role.DIETICIAN && dietician.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only dieticians or admins can view user diet plans");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested account is not a user");
        }

        if (dietician.getRole() == Role.ADMIN) {
            return dietPlanRepository.findByUserOrderByCreatedAtDesc(user)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        accessControlService.requireAssignedDietician(dietician, user);
        return dietPlanRepository.findByUserAndDieticianOrderByCreatedAtDesc(user, dietician)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DietPlanResponse> getDieticianPlans(String email) {
        User dietician = getAuthenticatedUser(email);
        requireRole(dietician, Role.DIETICIAN, "Only dieticians can view their weekly meal plans");

        return dietPlanRepository.findByDieticianOrderByCreatedAtDesc(dietician)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DietPlanResponse> getAllPlansForAdmin(String email) {
        User admin = getAuthenticatedUser(email);
        requireRole(admin, Role.ADMIN, "Only admins can view all weekly meal plans");
        return dietPlanRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User getAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requireRole(User user, Role role, String message) {
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private void validateWeeklyMeals(List<WeeklyMealRequest> meals) {
        if (meals == null || meals.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weekly meal plan must include meals for 7 days");
        }

        Set<String> mealKeys = meals.stream()
                .map(meal -> meal.getDayNumber() + ":" + meal.getMealType())
                .collect(Collectors.toSet());

        for (int day = 1; day <= 7; day++) {
            for (MealType mealType : List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)) {
                if (!mealKeys.contains(day + ":" + mealType)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weekly meal plan must include " + mealType + " for day " + day);
                }
            }
        }
    }

    private DietPlanResponse toResponse(DietPlan dietPlan) {
        User user = dietPlan.getUser();
        User dietician = dietPlan.getDietician();

        return DietPlanResponse.builder()
                .id(dietPlan.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .dieticianId(dietician.getId())
                .dieticianFullName(dietician.getFullName())
                .dieticianEmail(dietician.getEmail())
                .title(dietPlan.getTitle())
                .description(dietPlan.getDescription())
                .programGoal(dietPlan.getProgramGoal())
                .breakfast(dietPlan.getBreakfast())
                .lunch(dietPlan.getLunch())
                .dinner(dietPlan.getDinner())
                .snacks(dietPlan.getSnacks())
                .waterIntake(dietPlan.getWaterIntake())
                .calories(dietPlan.getCalories())
                .startDate(dietPlan.getStartDate())
                .endDate(dietPlan.getEndDate())
                .createdAt(dietPlan.getCreatedAt())
                .meals(mongoWeeklyMealPlanService.findByDietPlanId(dietPlan.getId())
                        .map(this::toMealResponses)
                        .orElseGet(() -> dietPlanMealRepository.findByDietPlanOrderByDayNumberAscMealTypeAsc(dietPlan)
                                .stream()
                                .map(this::toMealResponse)
                                .toList()))
                .build();
    }

    private WeeklyMealPlanDocument saveWeeklyMealPlanDocument(DietPlan dietPlan) {
        List<DietPlanMeal> meals = dietPlanMealRepository.findByDietPlanOrderByDayNumberAscMealTypeAsc(dietPlan);
        WeeklyMealPlanDocument saved = mongoWeeklyMealPlanService.saveWeeklyPlan(dietPlan, meals);
        auditLogService.record(
                dietPlan.getDietician().getId(),
                dietPlan.getDietician().getRole(),
                "MEAL_PLAN_CREATED",
                "WEEKLY_MEAL_PLANS",
                "Dietician " + dietPlan.getDietician().getId() + " created weekly meal plan " + dietPlan.getId()
        );
        return saved;
    }

    private DietPlanMealResponse toMealResponse(DietPlanMeal meal) {
        return DietPlanMealResponse.builder()
                .id(meal.getId())
                .dayNumber(meal.getDayNumber())
                .date(meal.getDietPlan().getStartDate() == null || meal.getDayNumber() == null
                        ? null
                        : meal.getDietPlan().getStartDate().plusDays(meal.getDayNumber() - 1L))
                .mealType(meal.getMealType())
                .mealName(meal.getMealName())
                .mealTime(meal.getMealTime())
                .waterIntake(meal.getWaterIntake())
                .instructions(meal.getInstructions())
                .build();
    }

    private List<DietPlanMealResponse> toMealResponses(WeeklyMealPlanDocument document) {
        if (document.getMealDetails() == null) {
            return List.of();
        }
        return document.getMealDetails()
                .stream()
                .map(meal -> toMealResponse(document, meal))
                .toList();
    }

    private DietPlanMealResponse toMealResponse(WeeklyMealPlanDocument document, WeeklyMealPlanDocument.WeeklyMealDetail meal) {
        return DietPlanMealResponse.builder()
                .id(meal.getMysqlMealId())
                .dayNumber(meal.getDayNumber())
                .date(document.getStartDate() == null || meal.getDayNumber() == null
                        ? null
                        : document.getStartDate().plusDays(meal.getDayNumber() - 1L))
                .mealType(meal.getMealType())
                .mealName(meal.getMealName())
                .mealTime(meal.getMealTime())
                .waterIntake(meal.getWaterIntake())
                .instructions(meal.getInstructions())
                .build();
    }

}
