package com.nutricare.nutricarebackend.dto;

import com.nutricare.nutricarebackend.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentStatusUpdateRequest {

    @NotNull
    private AppointmentStatus status;

    private String reason;

    private java.time.LocalDate appointmentDate;

    private java.time.LocalTime appointmentTime;
}
