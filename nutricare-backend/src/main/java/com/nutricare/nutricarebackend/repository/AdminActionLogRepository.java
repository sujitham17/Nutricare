package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
}
