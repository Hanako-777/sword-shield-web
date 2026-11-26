package com.swordshield.ai.service;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 离线规则引擎：不依赖网络与密钥，按问题关键词返回大会场景话术。
 * 它同时是真实大模型调用失败时的降级方案。
 */
@Component
public class MockAiProvider implements AiProvider {

    @Override
    public ChatResult chat(String systemPrompt, String userMessage) {
        String q = userMessage == null ? "" : userMessage.toLowerCase();
        String context = extractContext(systemPrompt);
        String answer;

        if (containsAny(q, "报名", "注册", "参会", "怎么去", "参加")) {
            answer = "报名参会分三步：\n"
                    + "1）在「注册」页创建账号（用户名 + 密码，密码使用 BCrypt 加密存储）；\n"
                    + "2）登录后进入「活动」页选择场次，点击「立即报名」；\n"
                    + "3）报名记录会自动带上渠道来源（web / 小程序 / 现场），可在「个人中心」查看。\n"
                    + "如需纸质票或资料包，可到「购票」页完成在线支付。";
        } else if (containsAny(q, "日程", "议程", "时间", "什么时候", "安排", "场次")) {
            answer = "本届大会重点场次如下：\n" + context
                    + "\n完整议程以「活动」页实时数据为准，开场前 30 分钟会推送直播入口。";
        } else if (containsAny(q, "直播", "回放", "视频", "观看")) {
            answer = "主论坛与分论坛均提供直播，活动详情页的「直播与回放」区域可直接进入；"
                    + "直播结束后 2 小时内上传回放，回放与嘉宾 PPT 会同步到「资料」页。";
        } else if (containsAny(q, "资料", "ppt", "白皮书", "下载", "课件")) {
            answer = "大会嘉宾 PPT 与《AI 安全实践白皮书》已上传到「资料」页，登录后可下载；"
                    + "资料由文件服务统一存储，并使用带时效的预签名地址，避免链接被外传。";
        } else if (containsAny(q, "地点", "地址", "展馆", "交通", "杭州")) {
            answer = "线下会场位于杭州国际博览中心（杭州市萧山区奔竞大道 353 号）。"
                    + "地铁 6 号线「博览中心」站步行约 600 米；自驾可停 P1/P2 停车场。";
        } else if (containsAny(q, "签到", "积分", "打卡", "抽奖")) {
            answer = "会中签到入口在「签到」页：每日签到 +10 积分，连续签到与现场打卡可参与积分抽奖；"
                    + "积分记录在个人中心可查。";
        } else if (containsAny(q, "支付", "购票", "票价", "多少钱", "订单", "发票")) {
            answer = "「购票」页提供大会通票、分论坛通行证与资料包三种商品。"
                    + "下单后会生成订单号，进入收银台完成支付，支付回调成功后订单状态变为“已支付”，全程幂等，重复回调不会重复扣款。";
        } else if (containsAny(q, "安全", "大模型", "ai", "主题", "论剑")) {
            answer = "西湖论剑·数字安全大会聚焦 AI 安全、数据要素与关键信息基础设施保护。"
                    + "本届设置「AI 大模型安全与治理」分论坛，讨论提示注入、模型供应链与合规落地。";
        } else if (containsAny(q, "你好", "hi", "hello", "在吗", "介绍")) {
            answer = "你好，我是西湖论剑·数字安全大会智能助手。"
                    + "我可以帮你查日程、报名、直播回放、资料下载、签到积分和购票支付。";
        } else {
            answer = "我理解你想了解「" + userMessage + "」。"
                    + "目前我能回答大会日程、报名、直播、资料、签到、购票等问题；"
                    + "你也可以直接说“帮我看看日程”或“怎么报名”。";
        }
        return new ChatResult(answer, "mock-rules");
    }

    @Override
    public String name() {
        return "mock-rules";
    }

    private String extractContext(String systemPrompt) {
        if (systemPrompt == null) return "（暂无实时日程）";
        int idx = systemPrompt.indexOf("【实时日程】");
        if (idx < 0) return "（暂无实时日程）";
        String tail = systemPrompt.substring(idx + "【实时日程】".length()).trim();
        return tail.isBlank() ? "（暂无实时日程）" : tail;
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    static List<String> suggestions() {
        return List.of("帮我看看日程", "怎么报名参会？", "资料在哪里下载？", "购票支持哪些方式？");
    }
}
