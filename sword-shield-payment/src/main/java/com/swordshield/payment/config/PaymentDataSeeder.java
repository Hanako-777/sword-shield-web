package com.swordshield.payment.config;

import com.swordshield.payment.domain.PaymentProduct;
import com.swordshield.payment.repository.PaymentProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(name = "payment.seed", havingValue = "true", matchIfMissing = true)
public class PaymentDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(PaymentDataSeeder.class);

    private final PaymentProductRepository repo;

    public PaymentDataSeeder(PaymentProductRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            log.info("支付商品已存在，跳过初始化");
            return;
        }
        repo.saveAll(List.of(
                product("西湖论剑大会 · VIP 通票", "TICKET", new BigDecimal("1280.00"), 500,
                        "含主论坛 + 全部分论坛、午餐与大会资料包，可开具电子发票。", 1),
                product("AI 安全分论坛通行证", "TICKET", new BigDecimal("399.00"), 800,
                        "单场分论坛入场资格，含同声传译与茶歇。", 2),
                product("大会嘉宾 PPT 资料包", "MATERIAL", new BigDecimal("99.00"), 2000,
                        "主论坛与分论坛嘉宾授权 PPT 合集（PDF），会后 7 天内邮件发送。", 3)
        ));
        log.info("已初始化 {} 个支付商品", repo.count());
    }

    private PaymentProduct product(String name, String type, BigDecimal price, int stock,
                                   String desc, int sort) {
        PaymentProduct p = new PaymentProduct();
        p.setName(name);
        p.setType(type);
        p.setPrice(price);
        p.setStock(stock);
        p.setDescription(desc);
        p.setStatus(1);
        p.setSortOrder(sort);
        return p;
    }
}
