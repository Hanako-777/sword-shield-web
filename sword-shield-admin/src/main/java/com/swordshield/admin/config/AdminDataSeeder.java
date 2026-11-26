package com.swordshield.admin.config;

import com.swordshield.admin.domain.AdminUser;
import com.swordshield.admin.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 后台演示账号：admin / admin123。
 */
@Component
@ConditionalOnProperty(name = "demo.seed", havingValue = "true", matchIfMissing = true)
public class AdminDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminDataSeeder.class);

    private final AdminUserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminDataSeeder(AdminUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.findByUsername("admin").isPresent()) {
            log.info("管理员账号已存在，跳过初始化");
            return;
        }
        AdminUser a = new AdminUser();
        a.setUsername("admin");
        a.setPasswordHash(encoder.encode("admin123"));
        a.setNickname("大会管理员");
        a.setStatus(1);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        log.info("已创建管理员账号 admin/admin123");
    }
}
