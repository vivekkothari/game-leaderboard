package com.github.vivekkothari.redis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;

public class RedisConfig {

  public static final int DEFAULT_POOL_SIZE = 20;

  /** Redis cluster configuration endpoint. */
  @JsonProperty public String url;

  @JsonProperty public int poolSize = DEFAULT_POOL_SIZE;

  public Duration connectionTimeout = Duration.ofSeconds(1);
  public Duration socketTimeout = Duration.ofSeconds(1);

  public RedisConfig setUrl(String url) {
    this.url = url;
    return this;
  }
}
