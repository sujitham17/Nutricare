package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.ConsultationPayment;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationPaymentRepository extends JpaRepository<ConsultationPayment, Long> {

    Optional<ConsultationPayment> findByAppointment(Appointment appointment);

    List<ConsultationPayment> findByUserOrderByCreatedAtDesc(User user);

    List<ConsultationPayment> findByDieticianOrderByCreatedAtDesc(User dietician);

    List<ConsultationPayment> findAllByOrderByCreatedAtDesc();
}
