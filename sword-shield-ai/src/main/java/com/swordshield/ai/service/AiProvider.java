package com.swordshield.ai.service;

/**
 * 大模型提供方抽象：真实模型与离线规则引擎实现同一接口，
 * 保证答辩/演示环境没有网络或 API Key 时功能依然可用。
 */
public interface AiProvider {
    ChatResult chat(String systemPrompt, String userMessage);
    String name();

    record ChatResult(String answer, String model) {}
}
