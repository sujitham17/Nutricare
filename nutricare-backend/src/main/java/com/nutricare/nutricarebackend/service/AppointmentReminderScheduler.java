package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.BookingStatus;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private static final DateTimeFormatter SMS_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter SMS_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final List<AppointmentStatus> UPCOMING_STATUSES = List.of(
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.APPROVED
    );

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Value("${nutricare.appointment-reminder.window-minutes:60}")
    private int reminderWindowMinutes;

    @Scheduled(fixedRateString = "${nutricare.appointment-reminder.fixed-rate-ms:300000}")
    @Transactional
    public void sendOneHourAppointmentReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusMinutes(reminderWindowMinutes);

        List<Appointment> appointments = appointmentRepository
                .findByBookingStatusAndStatusIn(BookingStatus.CONFIRMED, UPCOMING_STATUSES)
                .stream()
                .filter(appointment -> isInsideReminderWindow(appointment, now, reminderWindowEnd))
                .filter(appointment -> !appointment.isSmsReminderSent())
                .toList();

        appointments.forEach(this::sendReminder);
        if (!appointments.isEmpty()) {
            appointmentRepository.saveAll(appointments);
            log.info("Processed {} appointment SMS reminders", appointments.size());
        }
    }

    private boolean isInsideReminderWindow(Appointment appointment, LocalDateTime now, LocalDateTime reminderWindowEnd) {
        LocalDateTime appointmentDateTime = LocalDateTime.of(
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime()
        );
        return !appointmentDateTime.isBefore(now) && !appointmentDateTime.isAfter(reminderWindowEnd);
    }

    private void sendReminder(Appointment appointment) {
        User user = appointment.getUser();
        User dietician = appointment.getDietician();
        
        String formattedDate = appointment.getAppointmentDate().format(SMS_DATE_FORMAT);
        String formattedTime = appointment.getAppointmentTime().format(SMS_TIME_FORMAT);
        
        String userMsg = "NutriCare: Your appointment with Dietician " + dietician.getFullName() + " is confirmed on " + formattedDate + " at " + formattedTime + ".";
        String dieticianMsg = "NutriCare: Appointment with " + user.getFullName() + " is confirmed on " + formattedDate + " at " + formattedTime + ".";

        boolean updated = false;

        // Send SMS to user and dietician
        if (!appointment.isSmsReminderSent()) {
            if (user.getPhone() != null && !user.getPhone().isBlank()) {
                try {
                    notificationService.sendSms(user, "Appointment Reminder", userMsg);
                } catch (Exception e) {
                    log.error("Failed to send user SMS reminder: {}", e.getMessage());
                }
            }
            if (dietician.getPhone() != null && !dietician.getPhone().isBlank()) {
                try {
                    notificationService.sendSms(dietician, "Appointment Reminder", dieticianMsg);
                } catch (Exception e) {
                    log.error("Failed to send dietician SMS reminder: {}", e.getMessage());
                }
            }
            appointment.setSmsReminderSent(true);
            updated = true;
        }

        if (updated) {
            appointment.setReminderSentAt(LocalDateTime.now());
            appointment.setReminderSent(true);
        }
    }
}
