package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.MealComplianceRejectRequest;
import com.nutricare.nutricarebackend.dto.MealComplianceRequest;
import com.nutricare.nutricarebackend.dto.MealComplianceResponse;
import com.nutricare.nutricarebackend.dto.MealComplianceSummaryResponse;
import com.nutricare.nutricarebackend.dto.WeeklyMealRequest;
import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.DietPlanMeal;
import com.nutricare.nutricarebackend.entity.MealCompliance;
import com.nutricare.nutricarebackend.entity.MealComplianceStatus;
import com.nutricare.nutricarebackend.entity.MealType;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.mongo.document.MealComplianceLogDocument;
import com.nutricare.nutricarebackend.repository.DietPlanMealRepository;
import com.nutricare.nutricarebackend.repository.DietPlanRepository;
import com.nutricare.nutricarebackend.repository.MealComplianceRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealComplianceService {

    private final UserRepository userRepository;
    private final DietPlanRepository dietPlanRepository;
    private final DietPlanMealRepository dietPlanMealRepository;
    private final MealComplianceRepository mealComplianceRepository;
    private final MongoMealComplianceService mongoMealComplianceService;
    private final AccessControlService accessControlService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public void createMealsAndCompliance(DietPlan dietPlan) {
        List<DietPlanMeal> meals = ensureMealsForPlan(dietPlan);
        meals.forEach(meal -> getOrCreateCompliance(meal, dietPlan.getUser()));
    }

    @Transactional
    public void createWeeklyMealsAndCompliance(DietPlan dietPlan, List<WeeklyMealRequest> weeklyMeals) {
        if (weeklyMeals == null || weeklyMeals.isEmpty()) {
            createMealsAndCompliance(dietPlan);
            return;
        }

        weeklyMeals.stream()
                .filter(meal -> meal.getMealName() != null && !meal.getMealName().isBlank())
                .forEach(meal -> createMealIfPresent(
                        dietPlan,
                        meal.getDayNumber(),
                        meal.getMealType(),
                        meal.getMealName(),
                        meal.getMealTime(),
                        meal.getWaterIntake(),
                        meal.getInstructions()
                ));

        dietPlanMealRepository.findByDietPlanOrderByDayNumberAscMealTypeAsc(dietPlan)
                .forEach(meal -> getOrCreateCompliance(meal, dietPlan.getUser()));
    }

    @Transactional
    public MealComplianceResponse markFollowed(String email, MealComplianceRequest request) {
        User user = getUserByEmail(email);
        accessControlService.requireRole(user, Role.USER, "Only users can update meal compliance");

        MealCompliance compliance = getOwnCompliance(user, request.getMealId());
        requirePending(compliance);
        compliance.setStatus(MealComplianceStatus.FOLLOWED);
        compliance.setReason(null);
        compliance.setSubmittedAt(LocalDateTime.now());
        MealCompliance saved = mealComplianceRepository.save(compliance);
        persistComplianceHistory(saved);

        DietPlanMeal meal = saved.getDietPlanMeal();
        notificationService.sendNotification(
                meal.getDietPlan().getDietician(),
                user,
                "Meal Followed",
                user.getFullName() + "\nfollowed the meal plan for:\n\n" + displayMealType(meal.getMealType()),
                "MEAL_FOLLOWED"
        );

        return toResponse(saved);
    }

    @Transactional
    public MealComplianceResponse markNotFollowed(String email, MealComplianceRejectRequest request) {
        User user = getUserByEmail(email);
        accessControlService.requireRole(user, Role.USER, "Only users can update meal compliance");

        MealCompliance compliance = getOwnCompliance(user, request.getMealId());
        requirePending(compliance);
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason is required when meal is not followed");
        }
        compliance.setStatus(MealComplianceStatus.NOT_FOLLOWED);
        compliance.setReason(request.getReason());
        compliance.setSubmittedAt(LocalDateTime.now());
        MealCompliance saved = mealComplianceRepository.save(compliance);
        persistComplianceHistory(saved);

        DietPlanMeal meal = saved.getDietPlanMeal();
        notificationService.sendNotification(
                meal.getDietPlan().getDietician(),
                user,
                "Meal Not Followed",
                user.getFullName() + " did not follow " + displayMealType(meal.getMealType()) + ". Reason: " + request.getReason(),
                "MEAL_NOT_FOLLOWED"
        );

        return toResponse(saved);
    }

    @Transactional
    public List<MealComplianceResponse> getMyCompliance(String email) {
        User user = getUserByEmail(email);
        accessControlService.requireRole(user, Role.USER, "Only users can view meal compliance");
        ensureComplianceForUser(user);

        return mealComplianceRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .sorted(complianceComparator())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<MealComplianceResponse> getUserCompliance(String email, Long userId) {
        User dietician = getUserByEmail(email);
        User user = getUserById(userId);
        accessControlService.requireAssignedDietician(dietician, user);
        ensureComplianceForUserAndDietician(user, dietician);

        return mealComplianceRepository.findByUserAndDietPlanMealDietPlanDieticianOrderByCreatedAtDesc(user, dietician)
                .stream()
                .sorted(complianceComparator())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<MealComplianceResponse> getDieticianCompliance(String email, MealComplianceStatus status) {
        User dietician = getUserByEmail(email);
        accessControlService.requireRole(dietician, Role.DIETICIAN, "Only dieticians can view meal compliance");
        ensureComplianceForDietician(dietician);

        return (status == null
                ? mealComplianceRepository.findByDietPlanMealDietPlanDieticianOrderByCreatedAtDesc(dietician)
                : mealComplianceRepository.findByDietPlanMealDietPlanDieticianAndStatusOrderByCreatedAtDesc(dietician, status))
                .stream()
                .sorted(complianceComparator())
                .map(this::toResponse)
                .toList();
    }

    public List<MealComplianceSummaryResponse> getSummary(User user) {
        return summarize(mealComplianceRepository.findByUserOrderByCreatedAtDesc(user));
    }

    public List<MealComplianceSummaryResponse> getSummary(User user, User dietician) {
        if (dietician == null) {
            return getSummary(user);
        }
        return summarize(mealComplianceRepository.findByUserAndDietPlanMealDietPlanDieticianOrderByCreatedAtDesc(user, dietician));
    }

    public int getOverallCompliancePercent(User user) {
        long total = mealComplianceRepository.countByUser(user);
        long followed = mealComplianceRepository.countByUserAndStatus(user, MealComplianceStatus.FOLLOWED);
        return percent(followed, total);
    }

    public int getOverallCompliancePercent(User user, User dietician) {
        if (dietician == null) {
            return getOverallCompliancePercent(user);
        }
        List<MealCompliance> compliances = mealComplianceRepository.findByUserAndDietPlanMealDietPlanDieticianOrderByCreatedAtDesc(user, dietician);
        long total = compliances.size();
        long followed = compliances.stream().filter(item -> item.getStatus() == MealComplianceStatus.FOLLOWED).count();
        return percent(followed, total);
    }

    public void ensureComplianceForReport(User user, User dietician) {
        if (dietician == null) {
            ensureComplianceForUser(user);
        } else {
            ensureComplianceForUserAndDietician(user, dietician);
        }
    }

    public List<MealComplianceResponse> getComplianceForReport(User user, User dietician) {
        if (dietician == null) {
            return mealComplianceRepository.findByUserOrderByCreatedAtDesc(user)
                    .stream()
                    .sorted(complianceComparator())
                    .map(this::toResponse)
                    .toList();
        }
        return mealComplianceRepository.findByUserAndDietPlanMealDietPlanDieticianOrderByCreatedAtDesc(user, dietician)
                .stream()
                .sorted(complianceComparator())
                .map(this::toResponse)
                .toList();
    }

    private List<MealComplianceSummaryResponse> summarize(List<MealCompliance> compliances) {
        return List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
                .stream()
                .map(mealType -> {
                    List<MealCompliance> items = compliances.stream()
                            .filter(item -> item.getMealType() == mealType)
                            .toList();
                    long total = items.size();
                    long followed = items.stream().filter(item -> item.getStatus() == MealComplianceStatus.FOLLOWED).count();
                    long notFollowed = items.stream().filter(item -> item.getStatus() == MealComplianceStatus.NOT_FOLLOWED).count();
                    long pending = items.stream().filter(item -> item.getStatus() == MealComplianceStatus.PENDING).count();
                    return MealComplianceSummaryResponse.builder()
                            .mealType(mealType.name())
                            .followed(followed)
                            .total(total)
                            .pending(pending)
                            .notFollowed(notFollowed)
                            .compliancePercent(percent(followed, total))
                            .build();
                })
                .toList();
    }

    private void ensureComplianceForUser(User user) {
        dietPlanRepository.findByUserOrderByCreatedAtDesc(user)
                .forEach(this::createMealsAndCompliance);
    }

    private void ensureComplianceForUserAndDietician(User user, User dietician) {
        dietPlanRepository.findByUserAndDieticianOrderByCreatedAtDesc(user, dietician)
                .forEach(this::createMealsAndCompliance);
    }

    private void ensureComplianceForDietician(User dietician) {
        dietPlanMealRepository.findByDietPlanDieticianOrderByDietPlanCreatedAtDescIdAsc(dietician)
                .forEach(meal -> getOrCreateCompliance(meal, meal.getDietPlan().getUser()));
    }

    private MealCompliance getOwnCompliance(User user, Long mealId) {
        DietPlanMeal meal = dietPlanMealRepository.findById(mealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diet plan meal not found"));

        if (!meal.getDietPlan().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another user's meal compliance");
        }

        return getOrCreateCompliance(meal, user);
    }

    private MealCompliance getOrCreateCompliance(DietPlanMeal meal, User user) {
        return mealComplianceRepository.findByDietPlanMealAndUser(meal, user)
                .map(existing -> {
                    if (existing.getDayNumber() == null) {
                        existing.setDayNumber(meal.getDayNumber());
                        return mealComplianceRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    return mealComplianceRepository.save(MealCompliance.builder()
                            .dietPlanMeal(meal)
                            .user(user)
                            .dayNumber(meal.getDayNumber())
                            .mealType(meal.getMealType())
                            .status(MealComplianceStatus.PENDING)
                            .build());
                });
    }

    private void requirePending(MealCompliance compliance) {
        if (compliance.getStatus() != MealComplianceStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Meal compliance is already submitted");
        }
    }

    private List<DietPlanMeal> ensureMealsForPlan(DietPlan dietPlan) {
        createMealIfPresent(dietPlan, 1, MealType.BREAKFAST, dietPlan.getBreakfast(), "Breakfast", null, null);
        createMealIfPresent(dietPlan, 1, MealType.LUNCH, dietPlan.getLunch(), "Lunch", null, null);
        createMealIfPresent(dietPlan, 1, MealType.DINNER, dietPlan.getDinner(), "Dinner", null, null);
        createMealIfPresent(dietPlan, 1, MealType.SNACK, dietPlan.getSnacks(), "Snack", null, null);
        return dietPlanMealRepository.findByDietPlanOrderByDayNumberAscMealTypeAsc(dietPlan);
    }

    private void createMealIfPresent(
            DietPlan dietPlan,
            Integer dayNumber,
            MealType mealType,
            String mealName,
            String mealTime,
            String waterIntake,
            String instructions
    ) {
        if (mealName == null || mealName.isBlank()) {
            return;
        }

        dietPlanMealRepository.findByDietPlanAndDayNumberAndMealType(dietPlan, dayNumber, mealType)
                .orElseGet(() -> dietPlanMealRepository.save(DietPlanMeal.builder()
                        .dietPlan(dietPlan)
                        .dayNumber(dayNumber)
                        .mealType(mealType)
                        .mealName(mealName)
                        .mealTime(mealTime == null || mealTime.isBlank() ? displayMealType(mealType) : mealTime)
                        .waterIntake(waterIntake)
                        .instructions(instructions)
                        .build()));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String displayMealType(MealType mealType) {
        String lower = mealType.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private int percent(long followed, long total) {
        if (total == 0) {
            return 0;
        }
        return Math.round((followed * 100f) / total);
    }

    private Comparator<MealCompliance> complianceComparator() {
        return Comparator
                .comparing((MealCompliance item) -> item.getDietPlanMeal().getDietPlan().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MealCompliance::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> item.getMealType().ordinal());
    }

    private void persistComplianceHistory(MealCompliance compliance) {
        DietPlanMeal meal = compliance.getDietPlanMeal();
        DietPlan dietPlan = meal.getDietPlan();
        MealComplianceLogDocument document = mongoMealComplianceService.saveSubmission(compliance);
        compliance.setMongoDocumentId(document.getId());
        mealComplianceRepository.save(compliance);
        auditLogService.record(
                compliance.getUser().getId(),
                compliance.getUser().getRole(),
                "MEAL_COMPLIANCE_SUBMITTED",
                "MEAL_COMPLIANCE",
                "User " + compliance.getUser().getId() + " submitted " + compliance.getStatus()
                        + " for meal " + meal.getId() + " in plan " + dietPlan.getId()
        );
    }

    private MealComplianceResponse toResponse(MealCompliance compliance) {
        DietPlanMeal meal = compliance.getDietPlanMeal();
        DietPlan dietPlan = meal.getDietPlan();
        User user = compliance.getUser();
        User dietician = dietPlan.getDietician();

        return MealComplianceResponse.builder()
                .id(compliance.getId())
                .mealId(meal.getId())
                .dietPlanId(dietPlan.getId())
                .dietPlanTitle(dietPlan.getTitle())
                .dayNumber(meal.getDayNumber())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .dieticianId(dietician.getId())
                .dieticianFullName(dietician.getFullName())
                .mealType(compliance.getMealType())
                .mealName(meal.getMealName())
                .mealTime(meal.getMealTime())
                .status(compliance.getStatus())
                .reason(compliance.getReason())
                .planStartDate(dietPlan.getStartDate())
                .planEndDate(dietPlan.getEndDate())
                .submittedAt(compliance.getSubmittedAt())
                .createdAt(compliance.getCreatedAt())
                .build();
    }
}
