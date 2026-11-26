package com.swordshield.payment.repository;

import com.swordshield.payment.domain.PaymentCallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentCallbackLogRepository extends JpaRepository<PaymentCallbackLog, Long> {
    List<PaymentCallbackLog> findByOrderNoOrderByCreatedAtDesc(String orderNo);
}
