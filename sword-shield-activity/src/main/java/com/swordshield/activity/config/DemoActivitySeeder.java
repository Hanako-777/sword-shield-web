package com.swordshield.activity.config;

import com.swordshield.activity.domain.Activity;
import com.swordshield.activity.domain.ActivityRegistration;
import com.swordshield.activity.repository.ActivityRegistrationRepository;
import com.swordshield.activity.repository.ActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示数据：第 15 届西湖论剑·数字安全大会（2026-12-18 ~ 12-20）。
 * 日期设置为未来，保证现场可以真实走通“报名 → 渠道溯源 → 统计”链路。
 */
@Component
@ConditionalOnProperty(name = "demo.seed", havingValue = "true", matchIfMissing = true)
public class DemoActivitySeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoActivitySeeder.class);

    private final ActivityRepository activityRepo;
    private final ActivityRegistrationRepository regRepo;

    public DemoActivitySeeder(ActivityRepository activityRepo, ActivityRegistrationRepository regRepo) {
        this.activityRepo = activityRepo;
        this.regRepo = regRepo;
    }

    @Override
    public void run(String... args) {
        if (activityRepo.count() > 0) {
            log.info("活动数据已存在，跳过初始化");
            return;
        }

        List<Activity> list = new ArrayList<>();
        list.add(activity("2026 西湖论剑·数字安全大会 · 主论坛",
                "第十五届西湖论剑·数字安全大会主论坛，聚焦 AI 安全、数据要素与关键信息基础设施保护，邀请院士、部委专家与头部企业 CISO 共话数字安全新范式。",
                "/covers/1.svg", "2026-12-18T01:00:00Z", "2026-12-18T09:00:00Z",
                "杭州国际博览中心 · 主会场", "大会组委会", 1200, 986, 2860));
        list.add(activity("AI 大模型安全与治理分论坛",
                "围绕大模型越狱、提示注入、模型供应链与合规治理展开，发布《AI 安全实践白皮书（2026）》。",
                "/covers/2.svg", "2026-12-19T05:30:00Z", "2026-12-19T09:30:00Z",
                "杭州国际博览中心 · 分会场 A", "安恒信息研究院", 300, 268, 1946));
        list.add(activity("数据安全与个人信息保护研讨会",
                "面向政企数据安全负责人，解读数据出境、分类分级与隐私计算落地方案。",
                "/covers/3.svg", "2026-12-19T01:00:00Z", "2026-12-19T04:00:00Z",
                "杭州国际博览中心 · 分会场 B", "数据安全工作组", 200, 132, 1212));
        list.add(activity("网络安全人才发展与高校专场",
                "高校网络安全学院院长与用人单位对话，发布《2026 网络安全人才需求报告》。",
                "/covers/4.svg", "2026-12-20T01:30:00Z", "2026-12-20T04:00:00Z",
                "杭州国际博览中心 · 分会场 C", "产教融合联盟", 260, 205, 988));
        list.add(activity("安全成果发布与展区开放日",
                "集中发布年度安全成果与创新产品，展区全天开放，支持拍照打卡与积分兑换。",
                "/covers/5.svg", "2026-12-20T05:00:00Z", "2026-12-20T09:00:00Z",
                "杭州国际博览中心 · 展区", "大会组委会", 800, 512, 756));

        Instant now = Instant.now();
        for (Activity a : list) {
            a.setCreatedAt(now);
            a.setUpdatedAt(now);
        }
        List<Activity> saved = activityRepo.saveAll(list);
        log.info("已初始化 {} 场大会活动", saved.size());

        // 预置约 1/4 容量的报名记录，让统计页与渠道溯源有可观察的数据
        String[] channels = {"web", "web", "miniprogram", "miniprogram", "miniprogram", "onsite", "onsite", "invite"};
        int total = 0;
        int actIndex = 0;
        for (Activity a : saved) {
            int count = a.getMaxRegistrations() == null ? 30 : Math.max(20, a.getMaxRegistrations() / 4);
            for (int i = 0; i < count; i++) {
                ActivityRegistration r = new ActivityRegistration();
                r.setActivityId(a.getId());
                r.setUserId(100000L + actIndex * 1000L + i);
                r.setChannel(channels[i % channels.length]);
                r.setRegisteredAt(now.minusSeconds(1800L * (i + 1)));
                regRepo.save(r);
                total++;
            }
            a.setCurrentRegistrations(count);
            activityRepo.save(a);
            actIndex++;
        }
        log.info("已初始化 {} 条报名记录（用于报名统计与渠道溯源演示）", total);
    }

    private Activity activity(String title, String summary, String cover, String start, String end,
                              String location, String speaker, int max, int current, int heat) {
        Activity a = new Activity();
        a.setTitle(title);
        a.setSummary(summary);
        a.setCoverUrl(cover);
        a.setStartTime(Instant.parse(start));
        a.setEndTime(Instant.parse(end));
        a.setLocation(location);
        a.setSpeaker(speaker);
        a.setLiveUrl("https://live.example.com/xhlj2026");
        a.setReplayUrl("https://live.example.com/xhlj2026/replay");
        a.setMaxRegistrations(max);
        a.setCurrentRegistrations(current);
        a.setHeat(heat);
        a.setStatus(1);
        return a;
    }
}
