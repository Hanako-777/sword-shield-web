package com.swordshield.payment.controller;

import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import com.swordshield.payment.domain.PaymentCallbackLog;
import com.swordshield.payment.domain.PaymentOrder;
import com.swordshield.payment.domain.PaymentProduct;
import com.swordshield.payment.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public Response<List<PaymentProduct>> products() {
        return Response.ok(service.products());
    }

    @PostMapping("/orders")
    public Response<PaymentOrder> create(@RequestHeader("Authorization") String authorization,
                                         @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(authorization);
        Long productId = ((Number) body.get("productId")).longValue();
        String channel = (String) body.get("channel");
        return Response.ok(service.createOrder(userId, productId, channel));
    }

    @GetMapping("/orders")
    public Response<Map<String, Object>> myOrders(@RequestHeader("Authorization") String authorization,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Long userId = requireUserId(authorization);
        Page<PaymentOrder> p = service.myOrders(userId, page, size);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }

    @GetMapping("/orders/{orderNo}")
    public Response<PaymentOrder> detail(@RequestHeader("Authorization") String authorization,
                                         @PathVariable String orderNo) {
        return Response.ok(service.getOrder(requireUserId(authorization), orderNo));
    }

    @PostMapping("/orders/{orderNo}/pay")
    public Response<Map<String, Object>> pay(@RequestHeader("Authorization") String authorization,
                                             @PathVariable String orderNo) {
        return Response.ok(service.initiatePay(requireUserId(authorization), orderNo));
    }

    /** 模拟收银台“确认支付成功”。真实项目由支付宝/微信的异步回调触发。 */
    @PostMapping("/orders/{orderNo}/confirm")
    public Response<PaymentOrder> confirm(@RequestHeader("Authorization") String authorization,
                                          @PathVariable String orderNo) {
        return Response.ok(service.confirmPay(requireUserId(authorization), orderNo));
    }

    /** 模拟第三方异步回调（网关白名单放行，生产环境必须验签）。 */
    @PostMapping("/callback/mock")
    public Response<Map<String, Object>> callback(@RequestBody Map<String, Object> body) {
        String orderNo = (String) body.get("orderNo");
        String tradeNo = (String) body.getOrDefault("tradeNo", "MOCK-TRADE-" + orderNo);
        BigDecimal amount = body.get("amount") == null ? null
                : new BigDecimal(String.valueOf(body.get("amount")));
        PaymentOrder o = service.handleCallback(orderNo, tradeNo, amount);
        return Response.ok(Map.of("orderNo", o.getOrderNo(), "status", o.getStatus()));
    }

    @GetMapping("/orders/{orderNo}/callbacks")
    public Response<List<PaymentCallbackLog>> callbacks(@RequestHeader("Authorization") String authorization,
                                                        @PathVariable String orderNo) {
        requireUserId(authorization);
        return Response.ok(service.callbackLogs(orderNo));
    }

    @GetMapping("/statistics")
    public Response<Map<String, Object>> statistics(@RequestHeader("Authorization") String authorization) {
        requireAdmin(authorization);
        return Response.ok(service.statistics());
    }

    private Long requireUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new SecurityException("未登录，请先登录");
        }
        try {
            String subject = JwtUtil.parseSubject(authorization.substring(7));
            if (subject.startsWith("admin_")) {
                return Long.valueOf(subject.substring("admin_".length()));
            }
            return Long.valueOf(subject);
        } catch (Exception e) {
            throw new SecurityException("登录状态已失效，请重新登录");
        }
    }

    private void requireAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new SecurityException("未登录，请先登录");
        }
        String subject = JwtUtil.parseSubject(authorization.substring(7));
        if (!subject.startsWith("admin_")) {
            throw new SecurityException("无权限，需要管理员账号");
        }
    }
}
