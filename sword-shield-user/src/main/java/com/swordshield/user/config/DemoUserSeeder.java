package com.swordshield.user.config;

import com.swordshield.user.domain.User;
import com.swordshield.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 演示账号：demo / 123456。首次启动自动创建，方便答辩现场直接登录。
 */
@Component
@ConditionalOnProperty(name = "demo.seed", havingValue = "true", matchIfMissing = true)
public class DemoUserSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DemoUserSeeder(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.findByUsername("demo").isPresent()) {
            log.info("演示账号已存在，跳过初始化");
            return;
        }
        User u = new User();
        u.setUsername("demo");
        u.setPasswordHash(encoder.encode("123456"));
        u.setNickname("演示用户");
        u.setGender("保密");
        u.setPhone("13800000000");
        u.setEmail("demo@xihulunjian.cn");
        u.setAvatarUrl("/static/default-avatar.png");
        u.setStatus(1);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        User saved = repo.save(u);
        log.info("已创建演示账号 demo/123456 userId={}", saved.getId());
    }
}
