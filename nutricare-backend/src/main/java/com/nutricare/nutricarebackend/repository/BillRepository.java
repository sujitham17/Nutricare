package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByBillTypeAndPaymentId(String billType, Long paymentId);
}
