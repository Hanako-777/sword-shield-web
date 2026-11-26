package com.swordshield.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swordshield.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议客户端（DeepSeek / 通义千问 compatible-mode / 本地 vLLM 均可）。
 * 只在配置了 ai.api-key 时启用；任何异常都由 AiService 降级到离线规则引擎。
 */
@Component
public class OpenAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    private final AiProperties props;
    private final ObjectMapper mapper;
    private final RestClient client;

    public OpenAiProvider(AiProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(Math.max(3000, props.getTimeoutMs()));
        this.client = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public boolean available() {
        return props.useRealModel();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChatResult chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("temperature", 0.3);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));

        long start = System.currentTimeMillis();
        Map<String, Object> resp = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + props.getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);

        String answer = extractContent(resp);
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("大模型返回内容为空");
        }
        log.info("LLM 调用成功 model={} cost={}ms", props.getModel(), System.currentTimeMillis() - start);
        return new ChatResult(answer.trim(), props.getModel());
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> resp) {
        if (resp == null) return null;
        Object choices = resp.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) return null;
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) return null;
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> msg)) return null;
        Object content = msg.get("content");
        return content == null ? null : String.valueOf(content);
    }

    @Override
    public String name() {
        return props.getModel();
    }
}
