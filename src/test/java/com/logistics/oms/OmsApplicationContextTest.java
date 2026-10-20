package com.logistics.oms;

import com.logistics.oms.cache.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 上下文装配冒烟测试（H2 内存库 + 进程内缓存，不依赖 MySQL/Redis）：
 * 验证全部 Bean 装配、SQL 初始化脚本、演示账号初始化均正常。
 */
@SpringBootTest
@ActiveProfiles("h2")
class OmsApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context);
        assertNotNull(context.getBean(CacheService.class));
        assertNotNull(context.getBean(PasswordEncoder.class));
        assertNotNull(context.getBean("jwtUtil"));
    }
}
