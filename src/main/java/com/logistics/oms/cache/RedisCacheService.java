package com.logistics.oms.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存实现：JSON 序列化，StringRedisTemplate 直连。
 */
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate template;
    private final ObjectMapper mapper;

    public RedisCacheService(StringRedisTemplate template, ObjectMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        template.opsForValue().set(key, toJson(value), ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        String json = template.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        template.delete(key);
    }

    @Override
    public boolean setIfAbsent(String key, Object value, long ttlSeconds) {
        Boolean ok = template.opsForValue()
                .setIfAbsent(key, toJson(value), ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存序列化失败", e);
        }
    }
}
