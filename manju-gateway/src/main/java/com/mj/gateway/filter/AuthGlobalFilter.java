package com.mj.gateway.filter;

import com.mj.common.context.UserContext;
import com.mj.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关全局认证拦截器
 * <p>
 * 基于 Spring Cloud Gateway 的 GlobalFilter 实现，对所有经过网关的请求进行 JWT 认证。
 * 认证通过后，将用户信息（userId、username）注入请求头转发给下游微服务。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    /**
     * 无需认证的路径白名单
     */
    private static final List<String> WHITELIST_PATHS = List.of(
            "/auth/login",
            "/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单路径直接放行
        if (isWhitelisted(path)) {
            log.debug("白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 2. 提取 Authorization 请求头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            log.warn("请求缺少 Authorization 头: {}", path);
            return unauthorized(exchange, "未提供认证Token");
        }

        // 3. 移除 "Bearer " 前缀获取 token
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        // 4. 校验 JWT Token
        if (!jwtUtils.validateToken(token)) {
            log.warn("Token 校验失败: {}", path);
            return unauthorized(exchange, "Token无效或已过期");
        }

        // 5. 提取用户信息
        Long userId = jwtUtils.getUserId(token);
        String username = jwtUtils.getUsername(token);

        // 6. 存入 UserContext，供当前请求链路使用
        UserContext.setUser(userId, username);

        // 7. 注入请求头转发给下游微服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Name", username)
                .build();

        log.debug("认证通过, userId: {}, username: {}, path: {}", userId, username, path);

        // 8. 传递修改后的请求，并在请求完成后清理 UserContext
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> UserContext.clear());
    }

    /**
     * 判断请求路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 返回 401 未认证响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":401,\"msg\":\"%s\",\"data\":null}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // 设置较高优先级，确保在其他过滤器之前执行
        return -100;
    }
}
