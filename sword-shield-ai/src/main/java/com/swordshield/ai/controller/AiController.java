package com.swordshield.ai.controller;

import com.swordshield.ai.config.AiProperties;
import com.swordshield.ai.service.AiService;
import com.swordshield.common.JwtUtil;
import com.swordshield.common.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService service;
    private final AiProperties props;

    public AiController(AiService service, AiProperties props) {
        this.service = service;
        this.props = props;
    }

    @PostMapping("/chat")
    public Response<Map<String, Object>> chat(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @RequestBody Map<String, String> body) {
        Long userId = parseUserId(auth);
        return Response.ok(service.chat(userId, body.get("sessionId"), body.get("message")));
    }

    @PostMapping("/poster-copy")
    public Response<Map<String, Object>> posterCopy(@RequestBody Map<String, String> body) {
        return Response.ok(service.posterCopy(body.get("title"), body.get("keywords")));
    }

    @GetMapping("/status")
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
    public Response<List<?>> history(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @RequestParam(value = "sessionId", required = false) String sessionId,
                                     @RequestParam(value = "size", defaultValue = "20") int size) {
        return Response.ok(service.history(parseUserId(auth), sessionId, size));
    }

    private Long parseUserId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        try {
            return Long.valueOf(JwtUtil.parseSubject(auth.substring(7)));
        } catch (Exception e) {
            return null;
        }
    }
}
