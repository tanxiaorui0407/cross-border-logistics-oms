package com.logistics.oms.security;

import com.logistics.oms.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JWT 工具测试：签发/解析回环、篡改拒绝、过期拒绝。
 */
class JwtUtilTest {

    private static final String SECRET = "CrossBorder-Logistics-OMS-2026-Jwt-Secret-Key-0123456789";

    @Test
    void roundTrip() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60);
        String token = jwtUtil.generateToken(new LoginUser(1L, "admin", "ADMIN", null));
        LoginUser parsed = jwtUtil.parseToken(token);
        assertEquals(1L, parsed.userId());
        assertEquals("admin", parsed.username());
        assertEquals("ADMIN", parsed.role());
        assertNull(parsed.customerId());
    }

    @Test
    void customerIdRoundTrip() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60);
        String token = jwtUtil.generateToken(new LoginUser(2L, "alice", "CUSTOMER", 1L));
        LoginUser parsed = jwtUtil.parseToken(token);
        assertEquals(1L, parsed.customerId());
    }

    @Test
    void tamperedTokenRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60);
        String token = jwtUtil.generateToken(new LoginUser(1L, "admin", "ADMIN", null));
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        BusinessException e = assertThrows(BusinessException.class, () -> jwtUtil.parseToken(tampered));
        assertEquals(401, e.getCode());
    }

    @Test
    void expiredTokenRejected() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 0); // 立即过期
        String token = jwtUtil.generateToken(new LoginUser(1L, "admin", "ADMIN", null));
        BusinessException e = assertThrows(BusinessException.class, () -> jwtUtil.parseToken(token));
        assertEquals(401, e.getCode());
    }
}
