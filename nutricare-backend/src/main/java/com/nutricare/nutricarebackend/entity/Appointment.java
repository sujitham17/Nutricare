package com.nutricare.nutricarebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dietician_id", nullable = false)
    private User dietician;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @OneToOne(mappedBy = "appointment", fetch = FetchType.EAGER)
    private ConsultationPayment payment;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(nullable = false, columnDefinition = "varchar(30) default 'PENDING'")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String paymentId;

    private String orderId;

    private LocalDateTime paidAt;

    @Column(nullable = false, columnDefinition = "varchar(30) default 'NOT_REQUIRED'")
    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus;

    private LocalDate refundExpectedBy;

    private Long cancelledBy;

    @Column(length = 1000)
    private String cancellationReason;

    private LocalDateTime cancelledAt;

    @Column(length = 1000)
    private String notes;

    @Column(length = 1000)
    private String meetingLink;

    @Column(nullable = false, columnDefinition = "varchar(30) default 'NOT_CREATED'")
    @Enumerated(EnumType.STRING)
    private MeetingStatus meetingStatus;

    private LocalDateTime meetingCreatedAt;

    private Integer userRating;

    @Column(length = 1000)
    private String userRatingComment;

    @Column(length = 2000)
    private String consultationNotes;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean reminderSent = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean smsReminderSent = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "bit default 0")
    private boolean whatsappReminderSent = false;

    private LocalDateTime reminderSentAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = AppointmentStatus.PENDING;
        }
        if (bookingStatus == null) {
            bookingStatus = BookingStatus.PENDING_APPROVAL;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
        if (refundStatus == null) {
            refundStatus = RefundStatus.NOT_REQUIRED;
        }
        if (meetingStatus == null) {
            meetingStatus = MeetingStatus.NOT_CREATED;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
