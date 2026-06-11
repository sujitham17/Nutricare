package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.AppointmentResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentResponse;
import com.nutricare.nutricarebackend.dto.DietPlanMealResponse;
import com.nutricare.nutricarebackend.dto.DietPlanResponse;
import com.nutricare.nutricarebackend.dto.HealthTrackingResponse;
import com.nutricare.nutricarebackend.dto.ReportSnapshotRequest;
import com.nutricare.nutricarebackend.dto.ReportSummaryResponse;
import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.ConsultationPayment;
import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.HealthTracking;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.mongo.document.WeeklyMealPlanDocument;
import com.nutricare.nutricarebackend.mongo.document.ReportSnapshotDocument;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.ConsultationPaymentRepository;
import com.nutricare.nutricarebackend.repository.DietPlanRepository;
import com.nutricare.nutricarebackend.repository.HealthTrackingRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String CURRENCY = "INR";

    private final UserRepository userRepository;
    private final HealthTrackingRepository healthTrackingRepository;
    private final AppointmentRepository appointmentRepository;
    private final DietPlanRepository dietPlanRepository;
    private final ConsultationPaymentRepository consultationPaymentRepository;
    private final SubscriptionService subscriptionService;
    private final AccessControlService accessControlService;
    private final MealComplianceService mealComplianceService;
    private final MongoWeeklyMealPlanService mongoWeeklyMealPlanService;
    private final MongoReportSnapshotService mongoReportSnapshotService;

    public ReportSummaryResponse getMySummary(String email) {
        User user = getUserByEmail(email);
        accessControlService.requireRole(user, Role.USER, "Only users can view their report");
        return buildUserReport(user, null);
    }

    public ReportSummaryResponse getUserSummary(String email, Long userId) {
        User dietician = getUserByEmail(email);
        User user = getUserById(userId);
        accessControlService.requireAssignedDietician(dietician, user);
        return buildUserReport(user, dietician);
    }

    public ReportSummaryResponse getDieticianSummary(String email) {
        User dietician = getUserByEmail(email);
        accessControlService.requireRole(dietician, Role.DIETICIAN, "Only dieticians can view this report");

        List<AppointmentResponse> appointments = appointmentRepository.findByDieticianOrderByAppointmentDateDescAppointmentTimeDesc(dietician)
                .stream()
                .map(this::toAppointmentResponse)
                .toList();

        List<DietPlanResponse> dietPlans = dietPlanRepository.findByDieticianOrderByCreatedAtDesc(dietician)
                .stream()
                .map(this::toDietPlanResponse)
                .toList();

        List<ConsultationPaymentResponse> payments = consultationPaymentRepository.findByDieticianOrderByCreatedAtDesc(dietician)
                .stream()
                .map(this::toPaymentResponse)
                .toList();

        return ReportSummaryResponse.builder()
                .userId(dietician.getId())
                .userFullName(dietician.getFullName())
                .userEmail(dietician.getEmail())
                .appointments(appointments)
                .dietPlans(dietPlans)
                .payments(payments)
                .totalPaid(payments.stream()
                        .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS)
                        .map(ConsultationPaymentResponse::getAmount)
                        .filter(value -> value != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .overallCompliancePercent(0)
                .appointmentCounts(appointments.stream()
                        .collect(Collectors.groupingBy(appointment -> appointment.getStatus().name(), Collectors.counting())))
                .build();
    }

    public ReportSnapshotDocument saveSnapshot(String email, ReportSnapshotRequest request) {
        User generatedBy = getUserByEmail(email);
        Long userId = request.getUserId();
        Long dieticianId = request.getDieticianId();

        if (generatedBy.getRole() == Role.USER) {
            userId = generatedBy.getId();
        } else if (generatedBy.getRole() == Role.DIETICIAN) {
            dieticianId = generatedBy.getId();
            if (userId != null) {
                accessControlService.requireAssignedDietician(generatedBy, getUserById(userId));
            }
        } else if (generatedBy.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot export reports");
        }

        return mongoReportSnapshotService.save(
                request.getReportType(),
                userId,
                dieticianId,
                generatedBy.getId(),
                generatedBy.getRole(),
                request.getData()
        );
    }

    private ReportSummaryResponse buildUserReport(User user, User dietician) {
        List<HealthTrackingResponse> healthRecords = healthTrackingRepository.findByUserOrderByRecordedDateDescCreatedAtDesc(user)
                .stream()
                .map(this::toHealthResponse)
                .toList();
        List<AppointmentResponse> appointments = appointmentRepository.findByUserOrderByAppointmentDateDescAppointmentTimeDesc(user)
                .stream()
                .filter(appointment -> dietician == null || appointment.getDietician().getId().equals(dietician.getId()))
                .map(this::toAppointmentResponse)
                .toList();
        List<DietPlanResponse> dietPlans = (dietician == null
                ? dietPlanRepository.findByUserOrderByCreatedAtDesc(user)
                : dietPlanRepository.findByUserAndDieticianOrderByCreatedAtDesc(user, dietician))
                .stream()
                .map(this::toDietPlanResponse)
                .toList();
        List<ConsultationPaymentResponse> payments = consultationPaymentRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .filter(payment -> dietician == null || payment.getDietician().getId().equals(dietician.getId()))
                .map(this::toPaymentResponse)
                .toList();
        mealComplianceService.ensureComplianceForReport(user, dietician);

        return ReportSummaryResponse.builder()
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .latestHealth(healthRecords.isEmpty() ? null : healthRecords.get(0))
                .subscription(subscriptionService.getCurrentSubscription(user))
                .healthRecords(healthRecords)
                .appointments(appointments)
                .dietPlans(dietPlans)
                .mealCompliance(mealComplianceService.getComplianceForReport(user, dietician))
                .mealComplianceSummary(mealComplianceService.getSummary(user, dietician))
                .payments(payments)
                .totalPaid(payments.stream()
                        .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS)
                        .map(ConsultationPaymentResponse::getAmount)
                        .filter(value -> value != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .overallCompliancePercent(mealComplianceService.getOverallCompliancePercent(user, dietician))
                .appointmentCounts(appointments.stream()
                        .collect(Collectors.groupingBy(appointment -> appointment.getStatus().name(), Collectors.counting())))
                .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private HealthTrackingResponse toHealthResponse(HealthTracking record) {
        User user = record.getUser();
        return HealthTrackingResponse.builder()
                .id(record.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .weight(record.getWeight())
                .height(record.getHeight())
                .bmi(record.getBmi())
                .bloodPressure(record.getBloodPressure())
                .sugarLevel(record.getSugarLevel())
                .waterIntake(record.getWaterIntake())
                .sleepHours(record.getSleepHours())
                .activityLevel(record.getActivityLevel())
                .notes(record.getNotes())
                .recordedDate(record.getRecordedDate())
                .createdAt(record.getCreatedAt())
                .age(user.getAge())
                .gender(user.getGender())
                .goal(user.getGoal())
                .build();
    }

    private AppointmentResponse toAppointmentResponse(Appointment appointment) {
        User user = appointment.getUser();
        User dietician = appointment.getDietician();
        java.util.Optional<ConsultationPayment> paymentOpt = consultationPaymentRepository.findByAppointment(appointment);
        PaymentStatus paymentStatus = paymentOpt.map(ConsultationPayment::getPaymentStatus).orElse(null);
        String paymentStatusText = paymentStatusText(paymentStatus);

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .dieticianId(dietician.getId())
                .dieticianFullName(dietician.getFullName())
                .dieticianEmail(dietician.getEmail())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .bookingStatus(appointment.getBookingStatus())
                .notes(appointment.getNotes())
                .meetingLink(appointment.getMeetingLink())
                .meetingStatus(appointment.getMeetingStatus())
                .meetingCreatedAt(appointment.getMeetingCreatedAt())
                .userRating(appointment.getUserRating())
                .userRatingComment(appointment.getUserRatingComment())
                .consultationNotes(appointment.getConsultationNotes())
                .createdAt(appointment.getCreatedAt())
                .paymentStatus(paymentStatus)
                .paymentStatusText(paymentStatusText)
                .build();
    }

    private DietPlanResponse toDietPlanResponse(DietPlan dietPlan) {
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
                        .orElse(null))
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

    private ConsultationPaymentResponse toPaymentResponse(ConsultationPayment payment) {
        User user = payment.getUser();
        User dietician = payment.getDietician();
        return ConsultationPaymentResponse.builder()
                .id(payment.getId())
                .appointmentId(payment.getAppointment().getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .dieticianId(dietician.getId())
                .dieticianFullName(dietician.getFullName())
                .amount(payment.getAmount())
                .platformCommissionRate(payment.getPlatformCommissionRate())
                .platformCommissionAmount(payment.getPlatformCommissionAmount())
                .dieticianEarningsAmount(payment.getDieticianEarningsAmount())
                .currency(CURRENCY)
                .paymentStatus(payment.getPaymentStatus())
                .paymentStatusText(paymentStatusText(payment.getPaymentStatus()))
                .paymentProvider(payment.getPaymentProvider())
                .providerPaymentId(payment.getProviderPaymentId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private String paymentStatusText(PaymentStatus status) {
        if (status == null) return "Not Paid";
        return status == PaymentStatus.SUCCESS ? "Paid" : "Not Paid";
    }
}
