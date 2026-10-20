package com.logistics.oms.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 进程内缓存实现（H2 快速演示模式 / 无 Redis 环境兜底）。
 * 懒过期：读取时校验 TTL。
 */
public class InMemoryCacheService implements CacheService {

    private static final class Entry {
        private final String json;
        private final long expireAt;

        Entry(String json, long expireAt) {
            this.json = json;
            this.expireAt = expireAt;
        }

        String json() {
            return json;
        }

        long expireAt() {
            return expireAt;
        }
    }

    private final ConcurrentMap<String, Entry> store = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public InMemoryCacheService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        store.put(key, new Entry(toJson(value), System.currentTimeMillis() + ttlSeconds * 1000L));
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Entry e = store.get(key);
        if (e == null) {
            return null;
        }
        if (e.expireAt() < System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        try {
            return mapper.readValue(e.json(), clazz);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public boolean setIfAbsent(String key, Object value, long ttlSeconds) {
        return store.putIfAbsent(key, new Entry(toJson(value),
                System.currentTimeMillis() + ttlSeconds * 1000L)) == null;
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存序列化失败", e);
        }
    }
}
