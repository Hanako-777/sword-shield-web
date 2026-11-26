package com.swordshield.interaction.config;

import com.swordshield.interaction.domain.Comment;
import com.swordshield.interaction.domain.Like;
import com.swordshield.interaction.repository.CommentRepository;
import com.swordshield.interaction.repository.LikeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 评论/点赞演示数据，让详情页一打开就有真实互动氛围。
 */
@Component
@ConditionalOnProperty(name = "demo.seed", havingValue = "true", matchIfMissing = true)
public class DemoInteractionSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoInteractionSeeder.class);

    private final CommentRepository commentRepo;
    private final LikeRepository likeRepo;

    public DemoInteractionSeeder(CommentRepository commentRepo, LikeRepository likeRepo) {
        this.commentRepo = commentRepo;
        this.likeRepo = likeRepo;
    }

    @Override
    public void run(String... args) {
        if (commentRepo.count() > 0) {
            log.info("互动数据已存在，跳过初始化");
            return;
        }
        Instant now = Instant.now();
        comment(9001L, "activity", 1L, "去年参加过，主论坛的院士分享非常硬核，今年继续报名！", now.minusSeconds(7200));
        comment(9002L, "activity", 1L, "请问展区开放日需要单独报名吗？", now.minusSeconds(5400));
        comment(9003L, "activity", 2L, "最期待大模型越狱和提示注入的攻防演示。", now.minusSeconds(3600));
        comment(9004L, "article", 1L, "已经转发给团队，一起报名。", now.minusSeconds(1800));

        for (int i = 0; i < 12; i++) {
            like(9100L + i, "activity", 1L, now.minusSeconds(600 + i * 60L));
        }
        for (int i = 0; i < 7; i++) {
            like(9200L + i, "activity", 2L, now.minusSeconds(900 + i * 60L));
        }
        for (int i = 0; i < 5; i++) {
            like(9300L + i, "article", 1L, now.minusSeconds(1200 + i * 60L));
        }
        log.info("已初始化 {} 条评论、{} 个点赞", commentRepo.count(), likeRepo.count());
    }

    private void comment(Long userId, String type, Long targetId, String content, Instant at) {
        Comment c = new Comment();
        c.setUserId(userId);
        c.setTargetType(type);
        c.setTargetId(targetId);
        c.setContent(content);
        c.setCreatedAt(at);
        commentRepo.save(c);
    }

    private void like(Long userId, String type, Long targetId, Instant at) {
        Like l = new Like();
        l.setUserId(userId);
        l.setTargetType(type);
        l.setTargetId(targetId);
        l.setCreatedAt(at);
        likeRepo.save(l);
    }
}
