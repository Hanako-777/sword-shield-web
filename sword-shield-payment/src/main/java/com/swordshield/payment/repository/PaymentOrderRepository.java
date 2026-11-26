package com.swordshield.payment.repository;

import com.swordshield.payment.domain.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderNo(String orderNo);
    Page<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<PaymentOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<PaymentOrder> findByStatus(String status);
    long countByStatus(String status);
}
