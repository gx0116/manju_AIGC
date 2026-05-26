package com.mj.user.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 用于生成、解析和验证 JWT Token，实现用户身份认证
 */
@Component
public class JwtUtils {

    /**
     * JWT 签名密钥，用于加密和验证 Token
     */
    private final SecretKey secretKey;

    /**
     * Token 有效期（毫秒）
     */
    private final long expiration;

    /**
     * 构造函数，从配置文件中读取密钥和有效期
     *
     * @param secret     JWT 签名的密钥字符串
     * @param expiration Token 的有效期（毫秒）
     */
    public JwtUtils(@Value("${mj.jwt.secret}") String secret,
                    @Value("${mj.jwt.expiration}") long expiration) {
        // 使用 HMAC-SHA 算法生成安全的密钥
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token
     * 
     * @param userId   用户ID，作为 Token 的主题（subject）
     * @param username 用户名，存储在自定义声明中
     * @return 生成的 JWT Token 字符串
     */
    public String generateToken(Long userId, String username) {
        // 获取当前时间作为签发时间
        Date now = new Date();
        // 计算过期时间 = 当前时间 + 有效期
        Date expireDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))    // 设置主题为用户ID
                .claim("username", username)        // 添加自定义声明：用户名
                .issuedAt(now)                      // 设置签发时间
                .expiration(expireDate)             // 设置过期时间
                .signWith(secretKey)                // 使用密钥签名
                .compact();                         // 生成最终的 Token 字符串
    }

    /**
     * 解析 Token 获取 Claims（声明信息）
     * 
     * @param token JWT Token 字符串
     * @return Claims 对象，包含 Token 中的所有声明信息
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)              // 使用密钥验证 Token 签名
                .build()
                .parseSignedClaims(token)           // 解析已签名的 Token
                .getPayload();                      // 获取载荷部分（声明信息）
    }

    /**
     * 从 Token 中获取用户ID
     * 
     * @param token JWT Token 字符串
     * @return 用户ID
     */
    public Long getUserId(String token) {
        // 从 subject 字段获取用户ID并转换为 Long 类型
        return Long.valueOf(parseToken(token).getSubject());
    }

    /**
     * 从 Token 中获取用户名
     * 
     * @param token JWT Token 字符串
     * @return 用户名
     */
    public String getUsername(String token) {
        // 从自定义声明中获取用户名
        return parseToken(token).get("username", String.class);
    }

    /**
     * 验证 Token 是否有效
     * 
     * @param token JWT Token 字符串
     * @return true 表示 Token 有效，false 表示 Token 无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            // 尝试解析 Token，如果成功则说明 Token 有效
            parseToken(token);
            return true;
        } catch (Exception e) {
            // 捕获异常（如签名错误、过期等），说明 Token 无效
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效时间
     *
     * @param token JWT Token 字符串
     * @return 剩余有效时间（毫秒），如果已过期返回 0
     */
    public long getRemainingTTL(String token) {
        try {
            Claims claims = parseToken(token);
            long now = System.currentTimeMillis();
            long expiration = claims.getExpiration().getTime();
            return Math.max(0, expiration - now);
        } catch (Exception e) {
            return 0;
        }
    }
}
