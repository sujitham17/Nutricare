package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.dto.AdminDashboardSummaryResponse;
import com.nutricare.nutricarebackend.dto.AdminPaymentResponse;
import com.nutricare.nutricarebackend.dto.AppointmentResponse;
import com.nutricare.nutricarebackend.dto.ConsultationPaymentResponse;
import com.nutricare.nutricarebackend.dto.ReportSummaryResponse;
import com.nutricare.nutricarebackend.entity.Role;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ReportService reportService;
    private final AdminService adminService;

    public byte[] userReport(String email) {
        return reportWorkbook("User Report", Role.USER.name(), reportService.getMySummary(email));
    }

    public byte[] dieticianUserReport(String email, Long userId) {
        return reportWorkbook("Dietician User Report", Role.DIETICIAN.name(), reportService.getUserSummary(email, userId));
    }

    public byte[] dieticianReport(String email) {
        return reportWorkbook("Dietician Report", Role.DIETICIAN.name(), reportService.getDieticianSummary(email));
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
            return bytes(workbook);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate Excel report", ex);
        }
    }

    private List<String> appointmentRow(AppointmentResponse appointment) {
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
        sheet.createRow(1).createCell(0).setCellValue(title);
        sheet.createRow(2).createCell(0).setCellValue("Generated date: " + LocalDateTime.now());
        sheet.createRow(3).createCell(0).setCellValue("Role: " + role);
        sheet.createRow(4).createCell(0).setCellValue("Details: " + details);
        return 5;
    }

    private int writeTable(Sheet sheet, int startRow, List<String> headers, List<List<String>> rows) {
        Row header = sheet.createRow(startRow);
        for (int i = 0; i < headers.size(); i++) {
            header.createCell(i).setCellValue(headers.get(i));
        }
        int rowIndex = startRow + 1;
        for (List<String> values : rows) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < values.size(); i++) {
                row.createCell(i).setCellValue(values.get(i));
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
