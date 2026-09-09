package com.foodcommunity.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 签发与校验（java-jwt）。
 * 载荷：subject=用户ID，claim role=角色（0 用户 / 1 管理员）。
 */
@Component
public class JwtUtil {

    private final Algorithm algorithm;
    private final long expireHours;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-hours:24}") long expireHours) {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret 未配置或长度不足 32 位：请在 backend/.env 中设置 JWT_SECRET（≥32 字符）");
        }
        this.algorithm = Algorithm.HMAC256(secret.trim());
        this.expireHours = expireHours;
    }

    public String createToken(Long userId, Integer role) {
        Date expiresAt = new Date(System.currentTimeMillis() + expireHours * 3600_000L);
        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("role", role)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    /**
     * 校验并解析 token。非法/过期会抛 JWTVerificationException，由调用方处理。
     */
    public DecodedJWT verify(String token) {
        return JWT.require(algorithm).build().verify(token);
    }
}
