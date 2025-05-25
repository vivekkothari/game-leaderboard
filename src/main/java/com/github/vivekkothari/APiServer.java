package com.github.vivekkothari;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vivekkothari.redis.RedisClient;
import com.github.vivekkothari.redis.RedisConfig;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.internal.common.JacksonUtil;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.docs.DocService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APiServer {

  private static final ObjectMapper mapper = JacksonUtil.newDefaultObjectMapper();
  private static final Logger logger = LoggerFactory.getLogger(APiServer.class);

  private static final RedisClient redisClient =
      new RedisClient(new RedisConfig().setUrl("redis://127.0.0.1:6379"));

  public static void main(String[] args) throws JsonProcessingException {
    var gameEventProducer = gameEventProducer();

    var server =
        Server.builder()
            .http(4040)
            .service("/", (_, _) -> HttpResponse.of("Hello, Armeria!"))
            .annotatedService(gameService(gameEventProducer))
            .serviceUnder(
                "/docs",
                DocService.builder()
                    .exampleRequests(
                        GameService.class,
                        "gameComplete",
                        mapper.writeValueAsString(new GameService.Game("u2", 100, Instant.now())))
                    .build())
            .build();

    server.closeOnJvmShutdown(
        () -> {
          redisClient.close();
          gameEventProducer.close();
        });

    server.start().join();

    logger.info(
        "Server has been started. Serving game service at http://127.0.0.1:{}",
        server.activeLocalPort());
  }

  static GameEventProducer gameEventProducer() {
    return new GameEventProducer();
  }

  static GameService gameService(GameEventProducer producer) {
    return new GameService(producer, new TopScoreCalculator(redisClient));
  }
}
