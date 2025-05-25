package com.github.vivekkothari.redis;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.Pool;

public class RedisClient implements AutoCloseable {

  private final Pool<Jedis> jedisPool;
  private volatile boolean closed = false;

  public RedisClient(RedisConfig config) {
    Objects.requireNonNull(config, "config");
    var url = URI.create(Objects.requireNonNull(config.url, "Redis URL required"));
    var poolConfig = new GenericObjectPoolConfig<Jedis>();
    poolConfig.setMaxTotal(config.poolSize);
    poolConfig.setMaxIdle(config.poolSize);
    jedisPool =
        new JedisPool(
            poolConfig,
            JedisURIHelper.getHostAndPort(url),
            DefaultJedisClientConfig.builder()
                .user(JedisURIHelper.getUser(url))
                .password(JedisURIHelper.getPassword(url))
                .ssl(JedisURIHelper.isRedisSSLScheme(url))
                .connectionTimeoutMillis(Math.toIntExact(config.connectionTimeout.toMillis()))
                .socketTimeoutMillis(Math.toIntExact(config.socketTimeout.toMillis()))
                .build());
  }

  /** Execute the function on jedis. */
  public <T> T with(Function<Jedis, T> handler) {
    try (var jedis = jedisPool.getResource()) {
      return handler.apply(jedis);
    }
  }

  /**
   * Execute the consumer with the cluster.
   *
   * @see #with(Function)
   */
  public void use(Consumer<Jedis> handler) {
    try (var jedis = jedisPool.getResource()) {
      handler.accept(jedis);
    }
  }

  @Override
  public void close() {
    jedisPool.close();
    closed = true;
  }

}
