package com.owlite.worker.config;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConfig {
    private static final JedisPool pool = new JedisPool(
        new JedisPoolConfig(),
        AppConfig.get("redis.host"),
        AppConfig.getInt("redis.port")
    );

    public static JedisPool getPool() {
        return pool;
    }
}