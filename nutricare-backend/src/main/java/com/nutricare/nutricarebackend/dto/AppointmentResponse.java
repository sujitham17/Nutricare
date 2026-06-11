package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import com.nutricare.nutricarebackend.entity.BookingStatus;
import com.nutricare.nutricarebackend.entity.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.RefundStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private Boolean success;
    private Long id;
    private Long appointmentId;
    private Long userId;
    private String userFullName;
    private String userName;
    private String userEmail;
    private String userProfileImage;
    private String userProfileImageUrl;
    private Long dieticianId;
    private String dieticianFullName;
    private String dieticianEmail;
    private String dieticianProfileImage;
    private String dieticianProfileImageUrl;
    private Double dieticianAverageRating;
    private Long dieticianTotalRatings;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private AppointmentStatus status;
    private BookingStatus bookingStatus;
    private BigDecimal consultationFee;
    private PaymentStatus paymentStatus;
    private String paymentStatusText;
    private String paymentId;
    private String orderId;
    private LocalDateTime paidAt;
    private RefundStatus refundStatus;
    private LocalDate refundExpectedBy;
    private String cancelledBy;
    private Long cancelledById;
    private String cancelledByRole;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private String popupMessage;
    private String notes;
    private String meetingLink;
    private MeetingStatus meetingStatus;
    private LocalDateTime meetingCreatedAt;
    private Integer userRating;
    private String userRatingComment;
    private String consultationNotes;
    private LocalDateTime createdAt;
}
