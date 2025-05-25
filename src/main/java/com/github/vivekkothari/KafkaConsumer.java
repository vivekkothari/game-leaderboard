package com.github.vivekkothari;

import com.github.vivekkothari.redis.RedisClient;
import com.github.vivekkothari.redis.RedisConfig;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConsumer {

  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

  private static final int numOfConsumers = 3;
  private static final GameDao dao = new GameDao(JooqProvider.getDsl());
  private static final RedisClient redisClient =
      new RedisClient(new RedisConfig().setUrl("redis://127.0.0.1:6379"));
  private static final ExecutorService executor = Executors.newFixedThreadPool(numOfConsumers * 2);
  private static final TopScoreCalculator calculator = new TopScoreCalculator(redisClient);

  public static void main(String[] args) {
    logger.info("Starting Kafka Consumer...");

    IntStream.range(0, numOfConsumers)
        .forEach(_ -> executor.submit(gameEventConsumer()::startConsuming));
    IntStream.range(0, numOfConsumers)
        .forEach(_ -> executor.submit(gameCdcConsumer()::startConsuming));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("Shutting down Kafka Consumer...");
                  executor.shutdown();
                  redisClient.close();
                  logger.info("Kafka Consumer shutdown complete.");
                }));
  }

  static GameEventConsumer gameEventConsumer() {
    return new GameEventConsumer(dao);
  }

  static GameCdcConsumer gameCdcConsumer() {
    return new GameCdcConsumer(calculator);
  }
}
