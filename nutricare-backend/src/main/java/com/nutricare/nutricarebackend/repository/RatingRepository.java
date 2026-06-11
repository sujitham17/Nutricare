package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.entity.Rating;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByAppointment(Appointment appointment);

    Optional<Rating> findByAppointment(Appointment appointment);

    List<Rating> findByUserOrderByCreatedAtDesc(User user);

    List<Rating> findByDieticianOrderByCreatedAtDesc(User dietician);

    long countByDietician(User dietician);

    @Query("select avg(r.rating) from Rating r where r.dietician = :dietician")
    Double averageByDietician(@Param("dietician") User dietician);
}
