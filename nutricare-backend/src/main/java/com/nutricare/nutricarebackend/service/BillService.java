package com.nutricare.nutricarebackend.service;

import com.nutricare.nutricarebackend.entity.Bill;
import com.nutricare.nutricarebackend.entity.ConsultationPayment;
import com.nutricare.nutricarebackend.entity.PaymentStatus;
import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.SubscriptionTransaction;
import com.nutricare.nutricarebackend.entity.User;
import com.nutricare.nutricarebackend.entity.Appointment;
import com.nutricare.nutricarebackend.repository.BillRepository;
import com.nutricare.nutricarebackend.repository.ConsultationPaymentRepository;
import com.nutricare.nutricarebackend.repository.SubscriptionTransactionRepository;
import com.nutricare.nutricarebackend.repository.AppointmentRepository;
import com.nutricare.nutricarebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillRepository billRepository;
    private final SubscriptionTransactionRepository subscriptionTransactionRepository;
    private final ConsultationPaymentRepository consultationPaymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PdfGenerationService pdfGenerationService;

    public void createBillForSubscription(SubscriptionTransaction transaction) {
        if (transaction == null || transaction.getStatus() != PaymentStatus.SUCCESS) {
            return;
        }
        billRepository.findByBillTypeAndPaymentId("SUBSCRIPTION", transaction.getId())
                .orElseGet(() -> {
                    String billNo = "BILL-SUB-" + String.format("%06d", transaction.getId());
                    Bill bill = Bill.builder()
                            .billNumber(billNo)
                            .billType("SUBSCRIPTION")
                            .paymentId(transaction.getId())
                            .userId(transaction.getUser().getId())
                            .billGeneratedAt(LocalDateTime.now())
                            .build();
                    log.info("Saved subscription bill details in database: {}", billNo);
                    return billRepository.save(bill);
                });
    }

    public void createBillForConsultation(ConsultationPayment payment) {
        if (payment == null || payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            return;
        }
        billRepository.findByBillTypeAndPaymentId("CONSULTATION", payment.getId())
                .orElseGet(() -> {
                    String billNo = "BILL-CON-" + String.format("%06d", payment.getId());
                    Bill bill = Bill.builder()
                            .billNumber(billNo)
                            .billType("CONSULTATION")
                            .paymentId(payment.getId())
                            .userId(payment.getUser().getId())
                            .billGeneratedAt(LocalDateTime.now())
                            .build();
                    log.info("Saved consultation bill details in database: {}", billNo);
                    return billRepository.save(bill);
                });
    }

    public byte[] getBillPdfBySubscription(Long subscriptionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        SubscriptionTransaction transaction = subscriptionTransactionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription transaction not found"));

        // Auth check: Only payer user or admin can view subscription bills
        if (user.getRole() != Role.ADMIN && !transaction.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (transaction.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill is only available for successful payments");
        }

        // Retrieve or generate database bill entry
        Bill bill = billRepository.findByBillTypeAndPaymentId("SUBSCRIPTION", transaction.getId())
                .orElseGet(() -> {
                    String billNo = "BILL-SUB-" + String.format("%06d", transaction.getId());
                    return billRepository.save(Bill.builder()
                            .billNumber(billNo)
                            .billType("SUBSCRIPTION")
                            .paymentId(transaction.getId())
                            .userId(transaction.getUser().getId())
                            .billGeneratedAt(LocalDateTime.now())
                            .build());
                });

        return pdfGenerationService.generateSubscriptionBillPdf(transaction, bill.getBillNumber());
    }

    public byte[] getBillPdfByPayment(Long paymentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        ConsultationPayment payment = consultationPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation payment not found"));

        // Auth check: User (payer), Dietician (receiver) or Admin can view
        boolean isUser = payment.getUser().getId().equals(user.getId());
        boolean isDietician = payment.getDietician().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isUser && !isDietician && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill is only available for successful payments");
        }

        Bill bill = billRepository.findByBillTypeAndPaymentId("CONSULTATION", payment.getId())
                .orElseGet(() -> {
                    String billNo = "BILL-CON-" + String.format("%06d", payment.getId());
                    return billRepository.save(Bill.builder()
                            .billNumber(billNo)
                            .billType("CONSULTATION")
                            .paymentId(payment.getId())
                            .userId(payment.getUser().getId())
                            .billGeneratedAt(LocalDateTime.now())
                            .build());
                });

        return pdfGenerationService.generateConsultationBillPdf(payment, bill.getBillNumber());
    }

    public byte[] getBillPdfByAppointment(Long appointmentId, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        ConsultationPayment payment = consultationPaymentRepository.findByAppointment(appointment)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment associated with this appointment"));

        return getBillPdfByPayment(payment.getId(), userEmail);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
