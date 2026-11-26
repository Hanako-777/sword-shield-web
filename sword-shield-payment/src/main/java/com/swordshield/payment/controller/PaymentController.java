package com.swordshield.payment.controller;

import com.swordshield.common.AuthUtils;
import com.swordshield.common.Response;
import com.swordshield.payment.domain.PaymentCallbackLog;
import com.swordshield.payment.domain.PaymentOrder;
import com.swordshield.payment.domain.PaymentProduct;
import com.swordshield.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Tag(name = "支付", description = "商品、订单、收银台、支付回调与对账")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/products")
    @Operation(summary = "在售商品列表", description = "公开接口")
    public Response<List<PaymentProduct>> products() {
        return Response.ok(service.products());
    }

    @PostMapping("/orders")
    @Operation(summary = "创建订单", description = "需登录")
    public Response<PaymentOrder> create(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        Long productId = ((Number) body.get("productId")).longValue();
        String channel = (String) body.get("channel");
        return Response.ok(service.createOrder(userId, productId, channel));
    }

    @GetMapping("/orders")
    @Operation(summary = "我的订单", description = "需登录，按时间倒序")
    public Response<Map<String, Object>> myOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = AuthUtils.requireUserId(userIdHeader, authorization);
        Page<PaymentOrder> p = service.myOrders(userId, page, size);
        return Response.ok(Map.of(
                "items", p.getContent(),
                "total", p.getTotalElements(),
                "page", p.getNumber(),
                "size", p.getSize()
        ));
    }

    @GetMapping("/orders/{orderNo}")
    @Operation(summary = "订单详情", description = "需登录且只能查看自己的订单")
    public Response<PaymentOrder> detail(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderNo) {
        return Response.ok(service.getOrder(AuthUtils.requireUserId(userIdHeader, authorization), orderNo));
    }

    @PostMapping("/orders/{orderNo}/pay")
    @Operation(summary = "发起支付", description = "返回模拟收银台信息")
    public Response<Map<String, Object>> pay(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderNo) {
        return Response.ok(service.initiatePay(AuthUtils.requireUserId(userIdHeader, authorization), orderNo));
    }

    /** 模拟收银台“确认支付成功”。真实项目由支付宝/微信的异步回调触发。 */
    @PostMapping("/orders/{orderNo}/confirm")
    @Operation(summary = "模拟支付成功", description = "演示用，不产生真实扣款")
    public Response<PaymentOrder> confirm(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderNo) {
        return Response.ok(service.confirmPay(AuthUtils.requireUserId(userIdHeader, authorization), orderNo));
    }

    /** 模拟第三方异步回调（网关白名单放行，生产环境必须验签）。 */
    @PostMapping("/callback/mock")
    @Operation(summary = "模拟第三方回调", description = "幂等：重复回调不会重复扣库存")
    public Response<Map<String, Object>> callback(@RequestBody Map<String, Object> body) {
        String orderNo = (String) body.get("orderNo");
        String tradeNo = (String) body.getOrDefault("tradeNo", "MOCK-TRADE-" + orderNo);
        BigDecimal amount = body.get("amount") == null ? null
                : new BigDecimal(String.valueOf(body.get("amount")));
        PaymentOrder o = service.handleCallback(orderNo, tradeNo, amount);
        return Response.ok(Map.of("orderNo", o.getOrderNo(), "status", o.getStatus()));
    }

    @GetMapping("/orders/{orderNo}/callbacks")
    @Operation(summary = "订单回调流水", description = "需登录")
    public Response<List<PaymentCallbackLog>> callbacks(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderNo) {
        AuthUtils.requireUserId(userIdHeader, authorization);
        return Response.ok(service.callbackLogs(orderNo));
    }

    /** 管理员对账；路径进入网关 admin-paths，强制 ADMIN 角色。 */
    @GetMapping("/admin/statistics")
    @Operation(summary = "支付对账统计", description = "仅管理员")
    public Response<Map<String, Object>> statistics(
            @RequestHeader(value = "X-Auth-Subject", required = false) String subjectHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return Response.ok(service.statistics());
    }
}
