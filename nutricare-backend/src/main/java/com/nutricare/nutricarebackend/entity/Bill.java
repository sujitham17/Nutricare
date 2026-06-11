package com.nutricare.nutricarebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String billNumber;

    @Column(nullable = false)
    private LocalDateTime billGeneratedAt;

    @Column(nullable = false)
    private String billType; // "SUBSCRIPTION" or "CONSULTATION"

    @Column(nullable = false)
    private Long paymentId; // ConsultationPayment ID or SubscriptionTransaction ID

    @Column(nullable = false)
    private Long userId; // The payer's user ID

    @PrePersist
    void prePersist() {
        if (billGeneratedAt == null) {
            billGeneratedAt = LocalDateTime.now();
        }
    }
}
