package com.swordshield.ai.service;

import com.swordshield.ai.config.AiProperties;
import com.swordshield.ai.domain.AiChatLog;
import com.swordshield.ai.repository.AiChatLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiProperties props;
    private final OpenAiProvider openAi;
    private final MockAiProvider mock;
    private final AiChatLogRepository logRepo;
    private final RestClient activityClient;

    public AiService(AiProperties props, OpenAiProvider openAi, MockAiProvider mock, AiChatLogRepository logRepo) {
        this.props = props;
        this.openAi = openAi;
        this.mock = mock;
        this.logRepo = logRepo;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(2000);
        this.activityClient = RestClient.builder()
                .baseUrl(props.getActivityServiceUrl())
                .requestFactory(factory)
                .build();
    }

    public Map<String, Object> chat(Long userId, String sessionId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (message.length() > 2000) {
            throw new IllegalArgumentException("问题过长，请控制在 2000 字以内");
        }

        String context = loadActivityContext();
        String system = buildSystemPrompt(context);

        long start = System.currentTimeMillis();
        AiProvider.ChatResult result;
        String mode;
        try {
            if (openAi.available()) {
                result = openAi.chat(system, message.trim());
                mode = "llm";
            } else {
                result = mock.chat(system, message.trim());
                mode = "mock";
            }
        } catch (Exception e) {
            // 真实模型不可用时自动降级，演示不中断
            log.warn("大模型调用失败，降级到离线规则引擎: {}", e.toString());
            result = mock.chat(system, message.trim());
            mode = "fallback";
        }
        long latency = System.currentTimeMillis() - start;

        saveLog(userId, sessionId, message, result, latency);
        log.info("AI 问答完成 userId={} session={} mode={} model={} latency={}ms",
                userId, sessionId, mode, result.model(), latency);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("answer", result.answer());
        resp.put("model", result.model());
        resp.put("mode", mode);
        resp.put("latencyMs", latency);
        resp.put("suggestions", MockAiProvider.suggestions());
        return resp;
    }

    public Map<String, Object> posterCopy(String title, String keywords) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("海报标题不能为空");
        }
        String prompt = "请为网络安全大会宣传海报写一句主标语和两条副标语，风格专业、有号召力，"
                + "不超过 20 字/条。标题：" + title + "；关键词：" + (keywords == null ? "数字安全、AI 安全" : keywords);

        String copy;
        String mode;
        if (openAi.available()) {
            try {
                copy = openAi.chat("你是资深品牌文案，输出中文，直接给结果，不要解释。", prompt).answer();
                mode = "llm";
            } catch (Exception e) {
                log.warn("文案生成失败，使用模板: {}", e.toString());
                copy = templateCopy(title);
                mode = "fallback";
            }
        } else {
            copy = templateCopy(title);
            mode = "mock";
        }
        log.info("海报文案生成 title={} mode={}", title, mode);
        return Map.of("title", title, "copy", copy, "mode", mode);
    }

    public List<AiChatLog> history(Long userId, String sessionId, int size) {
        PageRequest page = PageRequest.of(0, Math.min(50, Math.max(1, size)));
        if (sessionId != null && !sessionId.isBlank()) {
            return logRepo.findBySessionIdOrderByCreatedAtDesc(sessionId, page).getContent();
        }
        if (userId == null) {
            return List.of();
        }
        return logRepo.findByUserIdOrderByCreatedAtDesc(userId, page).getContent();
    }

    private void saveLog(Long userId, String sessionId, String question, AiProvider.ChatResult result, long latency) {
        try {
            AiChatLog l = new AiChatLog();
            l.setUserId(userId);
            l.setSessionId(sessionId == null || sessionId.isBlank() ? "anonymous" : sessionId);
            l.setQuestion(question.length() > 1900 ? question.substring(0, 1900) : question);
            String answer = result.answer();
            l.setAnswer(answer.length() > 3900 ? answer.substring(0, 3900) : answer);
            l.setModel(result.model());
            l.setLatencyMs(latency);
            l.setCreatedAt(Instant.now());
            logRepo.save(l);
        } catch (Exception e) {
            // 流水落库失败不能影响用户问答
            log.warn("AI 调用流水保存失败: {}", e.toString());
        }
    }

    private String buildSystemPrompt(String activityContext) {
        return "你是“西湖论剑·数字安全大会”的官方智能助手。\n"
                + "大会信息：2026 年 12 月 18—20 日，杭州国际博览中心；主题聚焦 AI 安全、数据要素与关基保护。\n"
                + "回答要求：简体中文、分点、简洁（尽量 200 字内），不确定的信息要说明以官网为准。\n"
                + "【实时日程】\n" + activityContext;
    }

    /**
     * 从活动服务拉取实时日程，作为大模型的上下文（RAG 的轻量实现）。
     * 活动服务不可用时退回内置描述，不影响问答。
     */
    @SuppressWarnings("unchecked")
    private String loadActivityContext() {
        try {
            Map<String, Object> resp = activityClient.get()
                    .uri("/api/activity/list?page=0&size=6&orderBy=time")
                    .retrieve()
                    .body(Map.class);
            if (resp == null || !(resp.get("data") instanceof Map<?, ?> data)) {
                return fallbackContext();
            }
            Object items = data.get("items");
            if (!(items instanceof List<?> list) || list.isEmpty()) {
                return fallbackContext();
            }
            StringBuilder sb = new StringBuilder();
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) continue;
                sb.append("· ").append(m.get("title"))
                  .append("（").append(m.get("startTime")).append("，").append(m.get("location")).append("）\n");
            }
            return sb.length() == 0 ? fallbackContext() : sb.toString();
        } catch (Exception e) {
            log.debug("拉取活动上下文失败，使用内置日程: {}", e.toString());
            return fallbackContext();
        }
    }

    private String fallbackContext() {
        List<String> lines = new ArrayList<>();
        lines.add("· 主论坛：AI 安全与数字中国新范式");
        lines.add("· 分论坛 A：AI 大模型安全与治理");
        lines.add("· 分论坛 B：数据安全与个人信息保护");
        lines.add("· 高校专场：网络安全人才发展");
        return String.join("\n", lines) + "\n";
    }

    private String templateCopy(String title) {
        String[] variants = {
                title + "\n与千人共赴数字安全之约",
                title + "\n守正创新，论剑西湖",
                title + "\n让安全成为数字化的底色"
        };
        int idx = Math.floorMod(title.hashCode(), variants.length);
        return variants[idx] + "\n— 西湖论剑·数字安全大会";
    }
}
