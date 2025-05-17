package com.github.vivekkothari;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.internal.common.JacksonUtil;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.docs.DocService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final ObjectMapper mapper = JacksonUtil.newDefaultObjectMapper();
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final GameDao dao = new GameDao(JooqProvider.getDsl());

  public static void main(String[] args) throws JsonProcessingException {
    var gameEventProducer = gameEventProducer();
    GameEventConsumer gameEventConsumer = gameEventConsumer();

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
                        mapper.writeValueAsString(new GameService.Game(2, 100, Instant.now())))
                    .build())
            .build();

    Thread consumer = new Thread(gameEventConsumer::startConsuming);
    consumer.start();
    server.closeOnJvmShutdown(
        () -> {
          gameEventProducer.close();
          gameEventConsumer.close();
        });

    server.start().join();

    logger.info(
        "Server has been started. Serving game service at http://127.0.0.1:{}",
        server.activeLocalPort());
  }

  static GameEventProducer gameEventProducer() {
    return new GameEventProducer();
  }

  static GameEventConsumer gameEventConsumer() {
    return new GameEventConsumer(dao);
  }

  static GameService gameService(GameEventProducer producer) {
    return new GameService(producer, new TopScoreCalculator(dao));
  }
}
