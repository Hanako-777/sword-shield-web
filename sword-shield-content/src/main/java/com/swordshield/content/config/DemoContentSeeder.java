package com.swordshield.content.config;

import com.swordshield.content.domain.Achievement;
import com.swordshield.content.domain.ContentArticle;
import com.swordshield.content.domain.Exhibitor;
import com.swordshield.content.domain.Material;
import com.swordshield.content.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 资讯 / 展商 / 成果 / 资料 演示数据。
 * 资料文件放在 uploads/demo 下，由本地文件服务提供下载。
 */
@Component
@ConditionalOnProperty(name = "demo.seed", havingValue = "true", matchIfMissing = true)
public class DemoContentSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoContentSeeder.class);

    private final ContentArticleRepository articleRepo;
    private final ExhibitorRepository exhibitorRepo;
    private final AchievementRepository achievementRepo;
    private final MaterialRepository materialRepo;

    public DemoContentSeeder(ContentArticleRepository articleRepo,
                             ExhibitorRepository exhibitorRepo,
                             AchievementRepository achievementRepo,
                             MaterialRepository materialRepo) {
        this.articleRepo = articleRepo;
        this.exhibitorRepo = exhibitorRepo;
        this.achievementRepo = achievementRepo;
        this.materialRepo = materialRepo;
    }

    @Override
    public void run(String... args) {
        seedArticles();
        seedExhibitors();
        seedAchievements();
        seedMaterials();
    }

    private void seedArticles() {
        if (articleRepo.count() > 0) {
            log.info("资讯数据已存在，跳过初始化");
            return;
        }
        Instant now = Instant.now();
        articleRepo.saveAll(List.of(
                article("第十五届西湖论剑·数字安全大会将于 12 月 18 日开幕",
                        "大会以“AI 安全 · 数据要素 · 关基保护”为主题，设置 1 场主论坛、6 场分论坛与 3000㎡ 展区。",
                        "/covers/1.svg", "大会动态", "<p>第十五届西湖论剑·数字安全大会将于 2026 年 12 月 18 日至 20 日在杭州国际博览中心举行。</p><p>本届大会首次设立 AI 安全成果展区，并联合高校发布网络安全人才需求报告。</p>", now.minusSeconds(86400)),
                article("《AI 安全实践白皮书（2026）》发布：从模型供应链到运行时防护",
                        "白皮书系统梳理大模型全生命周期风险，提出 12 项可落地的安全基线。",
                        "/covers/2.svg", "前沿观察", "<p>白皮书指出，提示注入与模型供应链污染是当前最需关注的两类风险。</p><p>报告建议企业建立模型准入、运行时监测与应急响应三道防线。</p>", now.minusSeconds(172800)),
                article("高校专场：网络安全人才缺口如何补齐",
                        "来自 30 所高校的院长与 20 家企业 CISO 围绕产教融合展开对话。",
                        "/covers/4.svg", "大会动态", "<p>与会专家认为，实战化课程与认证体系是提升人才供给质量的关键。</p>", now.minusSeconds(259200)),
                article("媒体观察：数字安全大会为何成为行业“金名片”",
                        "从 2012 年创办至今，西湖论剑累计线下参会超 10000 人次、线上观看超 2500 万人次。",
                        "/covers/3.svg", "媒体报道", "<p>大会已成为链接产、教、研的重要平台，品牌影响力持续提升。</p>", now.minusSeconds(345600)),
                article("展商前瞻：10 家安全厂商将发布年度新品",
                        "覆盖数据安全、零信任、AI 安全运营等方向。",
                        "/covers/5.svg", "展商动态", "<p>展区开放日为 12 月 20 日，观众可凭报名二维码入场。</p>", now.minusSeconds(432000))
        ));
        log.info("已初始化 {} 条大会资讯", articleRepo.count());
    }

    private ContentArticle article(String title, String summary, String cover, String category,
                                   String body, Instant publishedAt) {
        ContentArticle a = new ContentArticle();
        a.setTitle(title);
        a.setSummary(summary);
        a.setCoverUrl(cover);
        a.setCategory(category);
        a.setBody(body);
        a.setStatus(1);
        a.setPublishedAt(publishedAt);
        return a;
    }

    private void seedExhibitors() {
        if (exhibitorRepo.count() > 0) {
            log.info("展商数据已存在，跳过初始化");
            return;
        }
        exhibitorRepo.saveAll(List.of(
                exhibitor("安恒信息", "安恒信息", "国内领先的网络信息安全产品和服务提供商，覆盖数据安全、云安全与安全运营。", "/covers/logo1.svg"),
                exhibitor("杭州数安科技", "数安科技", "专注数据分类分级与隐私计算，服务金融、政务客户。", "/covers/logo2.svg"),
                exhibitor("之江实验室", "之江实验室", "新型研发机构，聚焦智能计算与网络安全前沿研究。", "/covers/logo3.svg"),
                exhibitor("华东网络安全学院", "华网学院", "网络安全人才培养基地，提供实战化课程与认证。", "/covers/logo4.svg")
        ));
        log.info("已初始化 {} 家展商", exhibitorRepo.count());
    }

    private Exhibitor exhibitor(String name, String brand, String intro, String logo) {
        Exhibitor e = new Exhibitor();
        e.setName(name);
        e.setBrand(brand);
        e.setIntroduction(intro);
        e.setLogoUrl(logo);
        e.setWebsite("https://www.example.com");
        return e;
    }

    private void seedAchievements() {
        if (achievementRepo.count() > 0) {
            log.info("成果数据已存在，跳过初始化");
            return;
        }
        achievementRepo.saveAll(List.of(
                achievement("AI 安全实践白皮书（2026）", "覆盖大模型全生命周期风险与 12 项安全基线。"),
                achievement("2026 网络安全人才需求报告", "调研 300 家企业与 50 所高校，给出人才能力图谱。"),
                achievement("数据要素安全流通最佳实践", "总结政务、金融、医疗三类场景的落地路径。")
        ));
        log.info("已初始化 {} 项大会成果", achievementRepo.count());
    }

    private Achievement achievement(String title, String desc) {
        Achievement a = new Achievement();
        a.setTitle(title);
        a.setDescription(desc);
        return a;
    }

    private void seedMaterials() {
        if (materialRepo.count() > 0) {
            log.info("资料数据已存在，跳过初始化");
            return;
        }
        materialRepo.saveAll(List.of(
                material("主论坛：AI 安全与数字中国新范式", "大会组委会", "demo/xhlj2026-keynote.pdf", 829),
                material("分论坛：大模型安全与治理实践", "安恒信息研究院", "demo/xhlj2026-ai-security.pdf", 837),
                material("大会资料包使用说明", "大会组委会", "demo/xhlj2026-guide.pdf", 807)
        ));
        log.info("已初始化 {} 份大会资料", materialRepo.count());
    }

    private Material material(String title, String speaker, String objectName, long size) {
        Material m = new Material();
        m.setTitle(title);
        m.setSpeaker(speaker);
        m.setObjectName(objectName);
        m.setContentType("application/pdf");
        m.setSize(size);
        m.setCreatedAt(Instant.now());
        return m;
    }
}
