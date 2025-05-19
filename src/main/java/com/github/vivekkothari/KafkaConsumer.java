package com.github.vivekkothari;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;

public class KafkaConsumer {

  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

  private static final int numOfConsumers = 3;
  private static final GameDao dao = new GameDao(JooqProvider.getDsl());
  private static final Jedis jedis =
      new Jedis("localhost", 6379, DefaultJedisClientConfig.builder().build());
  private static final ExecutorService executor = Executors.newFixedThreadPool(numOfConsumers);
  private static final TopScoreCalculator calculator = new TopScoreCalculator(dao, jedis);

  public static void main(String[] args) {
    logger.info("Starting Kafka Consumer...");

    IntStream.range(0, numOfConsumers)
        .forEach(_ -> executor.submit(gameEventConsumer()::startConsuming));
    Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
  }

  static GameEventConsumer gameEventConsumer() {
    return new GameEventConsumer(calculator);
  }
}
