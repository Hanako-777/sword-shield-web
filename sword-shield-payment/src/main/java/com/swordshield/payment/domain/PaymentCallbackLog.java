package com.swordshield.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 支付回调流水。真实场景用于对账与排查“钱到了但订单没变”的问题。
 */
@Entity
@Table(name = "payment_callback_log", indexes = {
        @Index(name = "idx_callback_order", columnList = "order_no")
})
public class PaymentCallbackLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    private String channel;
    private String tradeNo;

    @Column(length = 2000)
    private String payload;

    private Boolean success;

    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTradeNo() { return tradeNo; }
    public void setTradeNo(String tradeNo) { this.tradeNo = tradeNo; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
