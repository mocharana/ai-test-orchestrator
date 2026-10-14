package com.wen.aitest.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发门票 / 验票取内容。
 *
 * @Component = 组件扫描成 Bean（它有 Spring 标签，不需要手工声明）
 * @Value     = 从 application.yml 读配置值灌进字段（jwt.secret / jwt.expire-hours）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours}")
    private long expireHours;

    /** 密钥对象：HMAC-SHA256 要求字节数 ≥32（yml 里的 secret 已保证） */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发门票：Payload 印上 用户名+签发时间+过期时间，用密钥盖防伪签名。
     * 对应概念课的三段：builder 自动生成 Header，subject/expiration 进 Payload，
     * signWith 生成 Signature，compact() 把三段用 . 拼成最终字符串。
     */
    public String generate(String username) {
        return Jwts.builder()
                .subject(username)                                              // sub：门票写给谁
                .issuedAt(new Date())                                           // iat：签发时间
                .expiration(new Date(System.currentTimeMillis() + expireHours * 3600_000L)) // exp：有效期
                .signWith(getKey())                                             // 签名
                .compact();                                                     // 三段拼接成字符串
    }

    /**
     * 验票：验签名 + 验过期一体完成（parseSignedClaims 内部先比对签名、再查 exp）。
     * 不合法（改过内容/过期/格式错）直接抛异常——M1-4 拦截器负责接住转成 401。
     * 合法则返回 Payload，取出用户名。
     */
    public String parseUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())       // 用同一把密钥重算签名比对
                .build()
                .parseSignedClaims(token)   // 验签 + 查过期
                .getPayload();              // 拿回 Payload
        return claims.getSubject();
    }
}
