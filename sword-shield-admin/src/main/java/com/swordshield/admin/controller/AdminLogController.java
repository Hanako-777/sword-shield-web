package com.swordshield.admin.controller;

import com.swordshield.admin.service.AdminLogService;
import com.swordshield.common.AuthUtils;
import com.swordshield.common.ErrorCode;
import com.swordshield.common.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "系统日志与审计（管理员）", description = "查看微服务全链路运行日志、AI 问答审计日志与支付回调流水")
public class AdminLogController {

    private final AdminLogService logService;

    public AdminLogController(AdminLogService logService) {
        this.logService = logService;
    }

    @GetMapping("/summary")
    @Operation(summary = "日志概览指标", description = "汇总各微服务日志文件状态、ERROR 数量、AI 问答审计数与支付回调统计")
    public Response<Map<String, Object>> summary(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return Response.ok(logService.getSummary());
    }

    @GetMapping("/runtime")
    @Operation(summary = "微服务运行日志（支持 TraceId 跨服务追踪）",
            description = "读取 9 个微服务的 Logback 运行日志或错误日志，支持按服务、日志级别、TraceId、关键词检索")
    public Response<Map<String, Object>> runtimeLogs(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @RequestParam(name = "service", required = false, defaultValue = "ALL") String service,
            @RequestParam(name = "level", required = false, defaultValue = "ALL") String level,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "traceId", required = false) String traceId,
            @RequestParam(name = "errorOnly", required = false, defaultValue = "false") boolean errorOnly,
            @RequestParam(name = "limit", required = false, defaultValue = "100") int limit) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        try {
            return Response.ok(logService.queryRuntimeLogs(service, level, keyword, traceId, errorOnly, limit));
        } catch (IllegalArgumentException ex) {
            return Response.error(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/ai")
    @Operation(summary = "AI 问答审计日志", description = "分页查询 ai_chat_log 表中的问答记录、模型与响应耗时")
    public Response<Map<String, Object>> aiChatLogs(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "model", required = false, defaultValue = "ALL") String model,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "15") int size) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return Response.ok(logService.queryAiChatLogs(keyword, model, page, size));
    }

    @GetMapping("/payment-callbacks")
    @Operation(summary = "支付回调审计日志", description = "分页查询 payment_callback_log 及关联订单信息")
    public Response<Map<String, Object>> paymentCallbackLogs(
            @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {admin_token}", required = true)
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestHeader(name = "X-Auth-Subject", required = false) String subjectHeader,
            @RequestParam(name = "orderNo", required = false) String orderNo,
            @RequestParam(name = "channel", required = false, defaultValue = "ALL") String channel,
            @RequestParam(name = "success", required = false) Integer success,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "15") int size) {
        AuthUtils.requireAdminSubject(subjectHeader, authorization);
        return Response.ok(logService.queryPaymentCallbackLogs(orderNo, channel, success, page, size));
    }
}
