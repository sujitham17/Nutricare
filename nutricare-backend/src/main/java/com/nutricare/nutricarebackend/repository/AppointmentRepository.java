package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.BookingStatus;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserAndBookingStatusAndPaymentStatusNot(
            User user,
            BookingStatus bookingStatus,
            PaymentStatus paymentStatus
    );

    List<Appointment> findAllByOrderByAppointmentDateDescAppointmentTimeDesc();

    List<Appointment> findByUserOrderByAppointmentDateDescAppointmentTimeDesc(User user);

    List<Appointment> findByDieticianOrderByAppointmentDateDescAppointmentTimeDesc(User dietician);

    List<Appointment> findByDieticianAndBookingStatusInOrderByAppointmentDateDescAppointmentTimeDesc(
            User dietician,
            List<BookingStatus> bookingStatuses
    );

    List<Appointment> findByDieticianAndBookingStatusOrderByAppointmentDateDescAppointmentTimeDesc(
            User dietician,
            BookingStatus bookingStatus
    );

    boolean existsByUserAndDieticianAndStatus(User user, User dietician, AppointmentStatus status);

    boolean existsByUserAndDieticianAndStatusIn(User user, User dietician, List<AppointmentStatus> statuses);

    boolean existsByDieticianAndAppointmentDateAndAppointmentTimeAndStatusIn(
            User dietician,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            List<AppointmentStatus> statuses
    );

    boolean existsByUserAndStatusNot(User user, AppointmentStatus status);

    boolean existsByDieticianAndStatusNot(User dietician, AppointmentStatus status);

    List<Appointment> findByReminderSentFalseAndBookingStatusAndStatusIn(
            BookingStatus bookingStatus,
            List<AppointmentStatus> statuses
    );

    List<Appointment> findByBookingStatusAndStatusIn(
            BookingStatus bookingStatus,
            List<AppointmentStatus> statuses
    );

    long countByStatus(AppointmentStatus status);

    long countByDietician(User dietician);

    long countByDieticianAndStatusIn(User dietician, List<AppointmentStatus> statuses);
}
