package com.logistics.oms.cache;

/**
 * 缓存抽象：Redis 实现（生产）/ 进程内实现（H2 演示），业务层无感知。
 * 用于：价卡缓存、下单幂等、发号等场景。
 */
public interface CacheService {

    void set(String key, Object value, long ttlSeconds);

    <T> T get(String key, Class<T> clazz);

    void delete(String key);

    /** 仅当 key 不存在时写入，返回是否写入成功（幂等/分布式锁语义）。 */
    boolean setIfAbsent(String key, Object value, long ttlSeconds);
}
