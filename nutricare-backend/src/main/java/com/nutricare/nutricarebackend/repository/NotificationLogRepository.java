package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findAllByOrderByCreatedAtDesc();

    boolean existsByUserIdAndTitleAndMessage(Long userId, String title, String message);
}
