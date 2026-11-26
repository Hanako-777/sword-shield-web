package com.swordshield.payment.service;

import com.swordshield.payment.domain.PaymentCallbackLog;
import com.swordshield.payment.domain.PaymentOrder;
import com.swordshield.payment.domain.PaymentProduct;
import com.swordshield.payment.repository.PaymentCallbackLogRepository;
import com.swordshield.payment.repository.PaymentOrderRepository;
import com.swordshield.payment.repository.PaymentProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentProductRepository productRepo;
    private final PaymentOrderRepository orderRepo;
    private final PaymentCallbackLogRepository callbackRepo;

    @Value("${payment.order-prefix:XHLJ}")
    private String orderPrefix;

    public PaymentService(PaymentProductRepository productRepo,
                          PaymentOrderRepository orderRepo,
                          PaymentCallbackLogRepository callbackRepo) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.callbackRepo = callbackRepo;
    }

    public List<PaymentProduct> products() {
        return productRepo.findByStatusOrderBySortOrderAsc(1);
    }

    @Transactional
    public PaymentOrder createOrder(Long userId, Long productId, String channel) {
        PaymentProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new IllegalArgumentException("商品已下架");
        }
        if (product.getStock() != null && product.getStock() <= 0) {
            throw new IllegalArgumentException("商品已售罄");
        }
        Instant now = Instant.now();
        PaymentOrder o = new PaymentOrder();
        o.setOrderNo(generateOrderNo());
        o.setUserId(userId);
        o.setProductId(product.getId());
        o.setProductName(product.getName());
        o.setAmount(product.getPrice());
        o.setStatus("CREATED");
        o.setChannel(normalizeChannel(channel));
        o.setCreatedAt(now);
        o.setUpdatedAt(now);
        orderRepo.save(o);
        log.info("创建支付订单 orderNo={} userId={} product={} amount={}",
                o.getOrderNo(), userId, product.getName(), o.getAmount());
        return o;
    }

    @Transactional
    public Map<String, Object> initiatePay(Long userId, String orderNo) {
        PaymentOrder o = ownedOrder(userId, orderNo);
        if ("PAID".equals(o.getStatus())) {
            return orderView(o);
        }
        if ("CLOSED".equals(o.getStatus())) {
            throw new IllegalArgumentException("订单已关闭，请重新下单");
        }
        if (o.getPayUrl() == null || o.getPayUrl().isBlank()) {
            o.setPayUrl("/payment/cashier?orderNo=" + o.getOrderNo());
            o.setUpdatedAt(Instant.now());
            orderRepo.save(o);
        }
        log.info("发起支付 orderNo={} channel={} amount={}", o.getOrderNo(), o.getChannel(), o.getAmount());
        return orderView(o);
    }

    /** 模拟用户在收银台完成支付（等价于第三方支付成功后的同步返回）。 */
    @Transactional
    public PaymentOrder confirmPay(Long userId, String orderNo) {
        PaymentOrder o = ownedOrder(userId, orderNo);
        return markPaid(o, "MOCK-" + o.getOrderNo(), "USER_CONFIRM", null);
    }

    /**
     * 第三方异步回调。幂等：重复回调直接返回已支付订单，不会重复扣库存/记账。
     */
    @Transactional
    public PaymentOrder handleCallback(String orderNo, String tradeNo, BigDecimal amount) {
        PaymentOrder o = orderRepo.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderNo));
        if ("PAID".equals(o.getStatus())) {
            log.info("重复支付回调，忽略 orderNo={}", orderNo);
            return o;
        }
        if (amount != null && o.getAmount().compareTo(amount) != 0) {
            saveCallback(o, tradeNo, "CALLBACK", false);
            throw new IllegalArgumentException("回调金额与订单金额不一致");
        }
        String payload = "{\"orderNo\":\"" + orderNo + "\",\"tradeNo\":\"" + tradeNo
                + "\",\"amount\":" + amount + "}";
        return markPaid(o, tradeNo, "CALLBACK", payload);
    }

    public PaymentOrder getOrder(Long userId, String orderNo) {
        return ownedOrder(userId, orderNo);
    }

    public Page<PaymentOrder> myOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<PaymentCallbackLog> callbackLogs(String orderNo) {
        return callbackRepo.findByOrderNoOrderByCreatedAtDesc(orderNo);
    }

    public Map<String, Object> statistics() {
        long total = orderRepo.count();
        long created = orderRepo.countByStatus("CREATED");
        List<PaymentOrder> paid = orderRepo.findByStatus("PAID");
        BigDecimal revenue = paid.stream()
                .map(PaymentOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Map<String, Object>> byProduct = new LinkedHashMap<>();
        for (PaymentOrder o : paid) {
            Map<String, Object> m = byProduct.computeIfAbsent(o.getProductName(), k -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("count", 0);
                x.put("revenue", BigDecimal.ZERO);
                return x;
            });
            m.put("count", (int) m.get("count") + 1);
            m.put("revenue", ((BigDecimal) m.get("revenue")).add(o.getAmount()));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalOrders", total);
        resp.put("paidOrders", paid.size());
        resp.put("pendingOrders", created);
        resp.put("revenue", revenue);
        resp.put("avgOrderValue", paid.isEmpty() ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(paid.size()), 2, java.math.RoundingMode.HALF_UP));
        resp.put("conversionRate", total == 0 ? 0
                : Math.round(paid.size() * 10000.0 / total) / 100.0);
        resp.put("byProduct", byProduct);
        return resp;
    }

    private PaymentOrder ownedOrder(Long userId, String orderNo) {
        PaymentOrder o = orderRepo.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!Objects.equals(o.getUserId(), userId)) {
            throw new IllegalArgumentException("无权操作该订单");
        }
        return o;
    }

    private PaymentOrder markPaid(PaymentOrder o, String tradeNo, String source, String payload) {
        if ("PAID".equals(o.getStatus())) {
            return o;
        }
        Instant now = Instant.now();
        o.setStatus("PAID");
        o.setTradeNo(tradeNo);
        o.setPaidAt(now);
        o.setUpdatedAt(now);
        orderRepo.save(o);

        if (o.getProductId() != null) {
            productRepo.findById(o.getProductId()).ifPresent(p -> {
                if (p.getStock() != null && p.getStock() > 0) {
                    p.setStock(p.getStock() - 1);
                    productRepo.save(p);
                }
            });
        }
        saveCallback(o, tradeNo, source, payload == null ? "{\"source\":\"" + source + "\"}" : payload);
        log.info("支付成功 orderNo={} tradeNo={} amount={} source={}",
                o.getOrderNo(), tradeNo, o.getAmount(), source);
        return o;
    }

    private void saveCallback(PaymentOrder o, String tradeNo, String channel, boolean success) {
        PaymentCallbackLog c = new PaymentCallbackLog();
        c.setOrderNo(o.getOrderNo());
        c.setChannel(channel);
        c.setTradeNo(tradeNo);
        c.setSuccess(success);
        c.setPayload("{\"orderNo\":\"" + o.getOrderNo() + "\",\"success\":" + success + "}");
        c.setCreatedAt(Instant.now());
        callbackRepo.save(c);
    }

    private void saveCallback(PaymentOrder o, String tradeNo, String channel, String payload) {
        PaymentCallbackLog c = new PaymentCallbackLog();
        c.setOrderNo(o.getOrderNo());
        c.setChannel(channel);
        c.setTradeNo(tradeNo);
        c.setPayload(payload);
        c.setSuccess(true);
        c.setCreatedAt(Instant.now());
        callbackRepo.save(c);
    }

    private Map<String, Object> orderView(PaymentOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderNo", o.getOrderNo());
        m.put("productId", o.getProductId());
        m.put("productName", o.getProductName());
        m.put("amount", o.getAmount());
        m.put("status", o.getStatus());
        m.put("channel", o.getChannel());
        m.put("payUrl", o.getPayUrl());
        m.put("tradeNo", o.getTradeNo());
        m.put("createdAt", o.getCreatedAt());
        m.put("paidAt", o.getPaidAt());
        return m;
    }

    private String generateOrderNo() {
        return orderPrefix + LocalDateTime.now().format(TS)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) return "MOCK";
        String c = channel.toUpperCase(Locale.ROOT);
        return switch (c) {
            case "ALIPAY", "WECHAT", "MOCK" -> c;
            default -> "MOCK";
        };
    }
}
