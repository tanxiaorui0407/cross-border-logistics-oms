package com.logistics.oms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.oms.cache.CacheService;
import com.logistics.oms.cache.InMemoryCacheService;
import com.logistics.oms.cache.RedisCacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存抽象：app.cache.redis=true 走 Redis（价卡/幂等），
 * false 走进程内缓存（H2 快速演示模式），实现业务层对缓存实现无感知。
 */
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis", havingValue = "true", matchIfMissing = true)
    public CacheService redisCacheService(StringRedisTemplate template, ObjectMapper mapper) {
        return new RedisCacheService(template, mapper);
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis", havingValue = "false")
    public CacheService inMemoryCacheService(ObjectMapper mapper) {
        return new InMemoryCacheService(mapper);
    }
}
