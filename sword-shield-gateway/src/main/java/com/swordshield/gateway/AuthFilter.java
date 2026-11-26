package com.swordshield.gateway;

import com.swordshield.common.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String auth = req.getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                JwtUtil.parseSubject(token);
                return chain.filter(exchange);
            } catch (Exception e) {
                log.debug("JWT 校验失败 path={} reason={}", path, e.getMessage());
            }
        }
        log.info("拒绝未授权访问 method={} path={}", req.getMethod(), path);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublic(String path) {
        return path.startsWith("/api/user/login")
                || path.startsWith("/api/user/register")
                || path.startsWith("/api/admin/auth/login")
                || path.startsWith("/api/activity/list")
                || path.startsWith("/api/activity/detail")
                || path.startsWith("/api/activity/statistics")
                || path.startsWith("/api/content/articles")
                || path.startsWith("/api/content/article")
                || path.startsWith("/api/content/exhibitors")
                || path.startsWith("/api/content/exhibitor")
                || path.startsWith("/api/content/achievements")
                || path.startsWith("/api/content/achievement")
                || path.startsWith("/api/content/poster")
                || path.startsWith("/api/ai/")
                || path.startsWith("/api/payment/products")
                || path.startsWith("/api/payment/callback/")
                || path.startsWith("/api/file/presigned")
                || path.startsWith("/api/file/raw")
                || path.startsWith("/api/interaction/comments")
                || path.startsWith("/api/interaction/likes/count");
    }

    @Override
    public int getOrder() { return -100; }
}
