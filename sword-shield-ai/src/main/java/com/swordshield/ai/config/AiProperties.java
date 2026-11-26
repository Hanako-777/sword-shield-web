package com.swordshield.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    /** 是否强制启用真实大模型（不配置时按 api-key 是否存在自动判断）。 */
    private boolean enabled = false;
    /** OpenAI 兼容接口地址，例如 https://api.deepseek.com 或 https://dashscope.aliyuncs.com/compatible-mode/v1 */
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private int timeoutMs = 15000;
    /** 活动服务地址，用于为问答拼接实时日程上下文。 */
    private String activityServiceUrl = "http://localhost:8102";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public String getActivityServiceUrl() { return activityServiceUrl; }
    public void setActivityServiceUrl(String activityServiceUrl) { this.activityServiceUrl = activityServiceUrl; }

    public boolean useRealModel() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
