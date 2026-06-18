package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.AdminDashboardSummaryResponse;
import com.nutricare.nutricarebackend.dto.AdminPaymentResponse;
import com.nutricare.nutricarebackend.dto.AppointmentResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentResponse;
import com.nutricare.nutricarebackend.dto.ReportSummaryResponse;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.DietPlan;
import com.nutricare.nutricarebackend.entity.DietPlanMeal;
import com.nutricare.nutricarebackend.entity.MealCompliance;
import com.nutricare.nutricarebackend.entity.MealType;
import com.nutricare.nutricarebackend.repository.UserRepository;
import com.nutricare.nutricarebackend.repository.DietPlanRepository;
import com.nutricare.nutricarebackend.repository.DietPlanMealRepository;
import com.nutricare.nutricarebackend.repository.MealComplianceRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ExcelExportService {

    private final ReportService reportService;
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final DietPlanRepository dietPlanRepository;
    private final DietPlanMealRepository dietPlanMealRepository;
    private final MealComplianceRepository mealComplianceRepository;
    private final MealComplianceService mealComplianceService;

    private byte[] generateEmptyReportExcel() {
        log.info("Workbook creation started");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");
            CellStyle titleStyle = titleStyle(workbook);
            Row brand = sheet.createRow(0);
            brand.createCell(0).setCellValue("NutriCare Report");
            brand.getCell(0).setCellStyle(titleStyle);
            
            Row msgRow = sheet.createRow(2);
            msgRow.createCell(0).setCellValue("No records found");
            log.info("Workbook creation completed");
            return bytes(workbook);
        } catch (IOException ex) {
            log.error("Error generating empty report workbook: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to generate Excel report", ex);
        }
    }

    public byte[] myDietPlanReport(String email) {
        log.info("myDietPlanReport service started for: {}", email);
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            log.info("myDietPlanReport - user id: {}, email: {}, role: {}, export type: my-diet-plan-excel", 
                    user.getId(), user.getEmail(), user.getRole().name());
            
            List<DietPlan> plans = dietPlanRepository.findByUserOrderByCreatedAtDesc(user);
            log.info("myDietPlanReport - plans fetched: {}", plans.size());

            if (plans.isEmpty()) {
                log.info("myDietPlanReport - no plans found, returning empty report");
                return generateEmptyReportExcel();
            }

            DietPlan activePlan = plans.get(0);
            log.info("myDietPlanReport - active plan ID: {}", activePlan.getId());
            mealComplianceService.ensureComplianceForReport(user, null);

            log.info("Workbook creation started");
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Diet Plan");
                CellStyle titleStyle = titleStyle(workbook);
                
                // Header
                Row brand = sheet.createRow(0);
                brand.createCell(0).setCellValue("NutriCare");
                brand.getCell(0).setCellStyle(titleStyle);
                
                Row titleRow = sheet.createRow(1);
                titleRow.createCell(0).setCellValue("Weekly Diet Plan Report");
                
                // General Info
                sheet.createRow(3).createCell(0).setCellValue("User Name: " + value(user.getFullName()));
                sheet.createRow(4).createCell(0).setCellValue("Dietician Name: " + value(activePlan.getDietician() != null ? activePlan.getDietician().getFullName() : ""));
                sheet.createRow(5).createCell(0).setCellValue("Plan Goal: " + value(activePlan.getProgramGoal()));
                
                String dateRange = "";
                if (activePlan.getStartDate() != null && activePlan.getEndDate() != null) {
                    dateRange = activePlan.getStartDate() + " to " + activePlan.getEndDate();
                }
                sheet.createRow(6).createCell(0).setCellValue("Week/Date Range: " + dateRange);
                sheet.createRow(7).createCell(0).setCellValue("Daily Calorie Target: " + value(activePlan.getCalories()) + " kcal");
                sheet.createRow(8).createCell(0).setCellValue("Daily Water Target: " + value(activePlan.getWaterIntake()));
                
                // Table starts at row 10
                List<String> headers = List.of("Day", "Date", "Meal Type", "Meal Name", "Meal Time", "Calories (kcal)", "Compliance Status", "Notes");
                List<DietPlanMeal> meals = dietPlanMealRepository.findByDietPlanOrderByDayNumberAscMealTypeAsc(activePlan);
                log.info("myDietPlanReport - meals fetched: {}", meals.size());
                
                List<List<String>> rows = new ArrayList<>();
                for (DietPlanMeal meal : meals) {
                    Optional<MealCompliance> complianceOpt = mealComplianceRepository.findByDietPlanMealAndUser(meal, user);
                    String complianceStatus = complianceOpt.map(c -> c.getStatus().name()).orElse("PENDING");
                    String notes = complianceOpt.map(MealCompliance::getReason).orElse("");
                    
                    String mealDateStr = "";
                    if (activePlan.getStartDate() != null && meal.getDayNumber() != null) {
                        mealDateStr = activePlan.getStartDate().plusDays(meal.getDayNumber() - 1L).toString();
                    }
                    
                    int mealCals = calculateMealCalories(meal.getMealType(), activePlan.getCalories());
                    
                    rows.add(List.of(
                            "Day " + value(meal.getDayNumber()),
                            mealDateStr,
                            value(meal.getMealType()),
                            value(meal.getMealName()),
                            value(meal.getMealTime()),
                            String.valueOf(mealCals),
                            complianceStatus,
                            notes
                    ));
                }
                
                writeTable(sheet, 10, headers, rows);
                autoSize(sheet, headers.size());
                log.info("Workbook creation completed");
                return bytes(workbook);
            }
        } catch (Throwable ex) {
            log.error("Throwable in myDietPlanReport: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private int calculateMealCalories(MealType mealType, Integer totalCalories) {
        if (totalCalories == null) return 0;
        double ratio = 0.0;
        if (mealType == MealType.BREAKFAST) {
            ratio = 0.25;
        } else if (mealType == MealType.LUNCH) {
            ratio = 0.35;
        } else if (mealType == MealType.SNACK) {
            ratio = 0.10;
        } else if (mealType == MealType.DINNER) {
            ratio = 0.30;
        }
        return (int) Math.round(totalCalories * ratio);
    }

    public byte[] userReport(String email) {
        log.info("userReport service started for: {}", email);
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            log.info("userReport - user id: {}, email: {}, role: {}, export type: user-report-excel", 
                    user.getId(), user.getEmail(), user.getRole().name());
            return reportWorkbook("User Report", Role.USER.name(), reportService.getMySummary(email));
        } catch (Throwable ex) {
            log.error("Throwable in userReport service: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public byte[] dieticianUserReport(String email, Long userId) {
        log.info("dieticianUserReport service started for dietician: {}, target user: {}", email, userId);
        try {
            User dietician = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dietician not found"));
            log.info("dieticianUserReport - dietician id: {}, email: {}, role: {}, export type: dietician-user-report-excel", 
                    dietician.getId(), dietician.getEmail(), dietician.getRole().name());
            return reportWorkbook("Dietician User Report", Role.DIETICIAN.name(), reportService.getUserSummary(email, userId));
        } catch (Throwable ex) {
            log.error("Throwable in dieticianUserReport service: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public byte[] dieticianReport(String email) {
        log.info("dieticianReport service started for: {}", email);
        try {
            User dietician = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dietician not found"));
            log.info("dieticianReport - dietician id: {}, email: {}, role: {}, export type: dietician-report-excel", 
                    dietician.getId(), dietician.getEmail(), dietician.getRole().name());
            return reportWorkbook("Dietician Report", Role.DIETICIAN.name(), reportService.getDieticianSummary(email));
        } catch (Throwable ex) {
            log.error("Throwable in dieticianReport service: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public byte[] adminReport() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Admin Report");
            CellStyle titleStyle = titleStyle(workbook);
            int rowIndex = writeHeader(sheet, titleStyle, "Admin Analytics Report", Role.ADMIN.name(), "Admin");

            AdminDashboardSummaryResponse summary = adminService.getDashboardSummary();
            rowIndex = writeTable(
                    sheet,
                    rowIndex + 1,
                    List.of("Metric", "Value"),
                    List.of(
                            List.of("Total Users", value(summary.getTotalUsers())),
                            List.of("Total Dieticians", value(summary.getTotalDieticians())),
                            List.of("Active User Subscriptions", value(summary.getActiveUserSubscriptions())),
                            List.of("Active Dietician Subscriptions", value(summary.getActiveDieticianSubscriptions())),
                            List.of("Total Appointments", value(summary.getTotalAppointments())),
                            List.of("Completed Appointments", value(summary.getCompletedAppointments())),
                            List.of("Subscription Revenue", value(summary.getSubscriptionRevenue())),
                            List.of("Consultation Revenue", value(summary.getConsultationRevenue())),
                            List.of("Admin Commission Revenue", value(summary.getAdminCommissionRevenue()))
                    )
            );

            List<List<String>> paymentRows = adminService.getPayments().stream()
                    .map(payment -> List.of(
                            value(payment.getId()),
                            value(payment.getPaymentType()),
                            value(payment.getPayerName()),
                            value(payment.getPayerRole()),
                            value(payment.getAmount()),
                            value(payment.getPaymentStatusText())
                    ))
                    .toList();
            writeTable(sheet, rowIndex + 2, List.of("ID", "Type", "Payer", "Role", "Amount", "Payment Status"), paymentRows);
            autoSize(sheet, 6);
            return bytes(workbook);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate Excel report", ex);
        }
    }

    private byte[] reportWorkbook(String title, String role, ReportSummaryResponse report) {
        log.info("reportWorkbook started - title: {}, role: {}", title, role);
        try {
            boolean hasData = false;
            if (report != null) {
                if (report.getAppointments() != null && !report.getAppointments().isEmpty()) hasData = true;
                if (report.getDietPlans() != null && !report.getDietPlans().isEmpty()) hasData = true;
                if (report.getPayments() != null && !report.getPayments().isEmpty()) hasData = true;
                if (report.getHealthRecords() != null && !report.getHealthRecords().isEmpty()) hasData = true;
            }
            if (!hasData) {
                log.info("reportWorkbook - no records found, returning empty report");
                return generateEmptyReportExcel();
            }

            int recordsCount = 0;
            if (report.getAppointments() != null) recordsCount += report.getAppointments().size();
            if (report.getDietPlans() != null) recordsCount += report.getDietPlans().size();
            if (report.getPayments() != null) recordsCount += report.getPayments().size();
            if (report.getHealthRecords() != null) recordsCount += report.getHealthRecords().size();
            log.info("reportWorkbook - records fetched: {}", recordsCount);

            log.info("Workbook creation started");
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Report");
                CellStyle titleStyle = titleStyle(workbook);
                int rowIndex = writeHeader(sheet, titleStyle, title, role, report.getUserFullName() + " <" + report.getUserEmail() + ">");

                rowIndex = writeTable(sheet, rowIndex + 1, List.of("Metric", "Value"), List.of(
                        List.of("User", value(report.getUserFullName())),
                        List.of("Email", value(report.getUserEmail())),
                        List.of("Total Paid", value(report.getTotalPaid())),
                        List.of("Overall Compliance", value(report.getOverallCompliancePercent()) + (report.getOverallCompliancePercent() == null ? "" : "%")),
                        List.of("Subscription", value(report.getSubscription() == null ? null : report.getSubscription().getPlanName()))
                ));

                List<List<String>> appointmentRows = report.getAppointments() == null ? List.of() : report.getAppointments().stream()
                        .map(this::appointmentRow)
                        .toList();
                rowIndex = writeTable(sheet, rowIndex + 2, List.of("Date", "Time", "Dietician", "Status", "Booking", "Payment"), appointmentRows);

                List<List<String>> paymentRows = report.getPayments() == null ? List.of() : report.getPayments().stream()
                        .map(this::paymentRow)
                        .toList();
                writeTable(sheet, rowIndex + 2, List.of("Payment ID", "Appointment", "Dietician", "Amount", "Status", "Date"), paymentRows);
                autoSize(sheet, 8);
                log.info("Workbook creation completed");
                return bytes(workbook);
            }
        } catch (Throwable ex) {
            log.error("Throwable in reportWorkbook: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private List<String> appointmentRow(AppointmentResponse appointment) {
        if (appointment == null) {
            return List.of("", "", "", "", "", "Not Paid");
        }
        return List.of(
                value(appointment.getAppointmentDate()),
                value(appointment.getAppointmentTime()),
                value(appointment.getDieticianFullName()),
                value(appointment.getStatus()),
                value(appointment.getBookingStatus()),
                formatPaymentStatusText(appointment.getPaymentStatusText() != null ? appointment.getPaymentStatusText() : appointment.getPaymentStatus())
        );
    }

    private List<String> paymentRow(ConsultationPaymentResponse payment) {
        if (payment == null) {
            return List.of("", "", "", "", "Not Paid", "");
        }
        return List.of(
                value(payment.getId()),
                value(payment.getAppointmentId()),
                value(payment.getDieticianFullName()),
                value(payment.getAmount()),
                formatPaymentStatusText(payment.getPaymentStatusText() != null ? payment.getPaymentStatusText() : payment.getPaymentStatus()),
                value(payment.getCreatedAt())
        );
    }

    private String formatPaymentStatusText(Object status) {
        if (status == null) return "Not Paid";
        String s = String.valueOf(status).trim().toUpperCase();
        if (s.equals("0") || s.equals("FALSE") || s.equals("PENDING") || s.equals("NOT_PAID") || s.equals("NOT PAID") || s.equals("FAILED")) {
            return "Not Paid";
        }
        if (s.equals("1") || s.equals("TRUE") || s.equals("SUCCESS") || s.equals("PAID")) {
            return "Paid";
        }
        return "Not Paid";
    }

    private int writeHeader(Sheet sheet, CellStyle titleStyle, String title, String role, String details) {
        Row brand = sheet.createRow(0);
        brand.createCell(0).setCellValue("NutriCare");
        brand.getCell(0).setCellStyle(titleStyle);
        sheet.createRow(1).createCell(0).setCellValue(value(title));
        sheet.createRow(2).createCell(0).setCellValue("Generated date: " + value(LocalDateTime.now()));
        sheet.createRow(3).createCell(0).setCellValue("Role: " + value(role));
        sheet.createRow(4).createCell(0).setCellValue("Details: " + value(details));
        return 5;
    }

    private int writeTable(Sheet sheet, int startRow, List<String> headers, List<List<String>> rows) {
        Row header = sheet.createRow(startRow);
        for (int i = 0; i < headers.size(); i++) {
            header.createCell(i).setCellValue(value(headers.get(i)));
        }
        int rowIndex = startRow + 1;
        for (List<String> values : rows) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < values.size(); i++) {
                row.createCell(i).setCellValue(value(values.get(i)));
            }
        }
        return rowIndex;
    }

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private byte[] bytes(XSSFWorkbook workbook) throws IOException {
        log.info("File stream started");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        byte[] result = out.toByteArray();
        log.info("File stream completed");
        return result;
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
