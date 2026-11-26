package com.swordshield.payment.repository;

import com.swordshield.payment.domain.PaymentProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentProductRepository extends JpaRepository<PaymentProduct, Long> {
    List<PaymentProduct> findByStatusOrderBySortOrderAsc(Integer status);
}
