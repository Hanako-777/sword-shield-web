package com.swordshield.ai.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 大模型调用流水：既用于问题排查，也用于运营侧统计“用户最关心什么”。
 */
@Entity
@Table(name = "ai_chat_log", indexes = {
        @Index(name = "idx_ai_log_user", columnList = "user_id"),
        @Index(name = "idx_ai_log_created", columnList = "created_at")
})
public class AiChatLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 游客可为空 */
    @Column(name = "user_id")
    private Long userId;

    private String sessionId;

    @Column(length = 2000)
    private String question;

    @Column(length = 4000)
    private String answer;

    /** mock / deepseek-chat / qwen-plus ... */
    private String model;

    private Long latencyMs;

    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
