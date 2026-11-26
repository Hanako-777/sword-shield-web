package com.swordshield.ai.controller;

import com.swordshield.ai.config.AiProperties;
import com.swordshield.ai.service.AiService;
import com.swordshield.common.AuthUtils;
import com.swordshield.common.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 助手", description = "大会智能问答与海报文案生成")
public class AiController {
    private final AiService service;
    private final AiProperties props;

    public AiController(AiService service, AiProperties props) {
        this.service = service;
        this.props = props;
    }

    @PostMapping("/chat")
    @Operation(summary = "智能问答", description = "游客可用；登录后会记录 userId，便于运营分析")
    public Response<Map<String, Object>> chat(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        return Response.ok(service.chat(resolveUserId(userIdHeader, authorization),
                body.get("sessionId"), body.get("message")));
    }

    @PostMapping("/poster-copy")
    @Operation(summary = "生成海报文案", description = "根据标题和关键词生成主标语/副标语")
    public Response<Map<String, Object>> posterCopy(@RequestBody Map<String, String> body) {
        return Response.ok(service.posterCopy(body.get("title"), body.get("keywords")));
    }

    @GetMapping("/status")
    @Operation(summary = "当前模型状态", description = "返回 mock 或真实大模型模式")
    public Response<Map<String, Object>> status() {
        return Response.ok(Map.of(
                "mode", props.useRealModel() ? "llm" : "mock",
                "model", props.useRealModel() ? props.getModel() : "mock-rules",
                "baseUrl", props.useRealModel() ? props.getBaseUrl() : "-",
                "note", props.useRealModel()
                        ? "已配置大模型 API，失败时自动降级到离线规则引擎"
                        : "未配置 AI_API_KEY，当前使用离线规则引擎（演示不受网络影响）"
        ));
    }

    @GetMapping("/history")
    @Operation(summary = "问答历史", description = "按 sessionId 或当前用户查询最近问答")
    public Response<List<?>> history(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return Response.ok(service.history(resolveUserId(userIdHeader, authorization), sessionId, size));
    }

    private Long resolveUserId(String userIdHeader, String authorization) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                return Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException ignored) {
                // fall through to JWT
            }
        }
        if (authorization != null && !authorization.isBlank()) {
            try {
                return AuthUtils.requireUserId(authorization);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
