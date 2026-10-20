package com.logistics.oms.security;

import com.logistics.oms.common.BusinessException;
import com.logistics.oms.common.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发 / 解析。
 * HS256 签名，secret 至少 32 字节；过期时间由配置 jwt.expire-minutes 控制。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMinutes;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-minutes}") long expireMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = expireMinutes;
    }

    public String generateToken(LoginUser user) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(user.username())
                .claim("uid", user.userId())
                .claim("role", user.role())
                .claim("customerId", user.customerId() == null ? 0 : user.customerId())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMinutes * 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token。过期/签名错误统一抛 401。
     */
    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Number uid = claims.get("uid", Number.class);
            Number customerId = claims.get("customerId", Number.class);
            return new LoginUser(
                    uid == null ? null : uid.longValue(),
                    claims.getSubject(),
                    claims.get("role", String.class),
                    customerId == null || customerId.longValue() <= 0 ? null : customerId.longValue());
        } catch (JwtException | IllegalArgumentException e) {
            throw BusinessException.unauthorized("登录已失效，请重新登录");
        }
    }
}
