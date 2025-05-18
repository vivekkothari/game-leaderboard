package com.github.vivekkothari;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;

public class KafkaConsumer {

  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

  private static final GameDao dao = new GameDao(JooqProvider.getDsl());
  private static final Jedis jedis =
      new Jedis("localhost", 6379, DefaultJedisClientConfig.builder().build());
  private static final TopScoreCalculator calculator = new TopScoreCalculator(dao, jedis);

  public static void main(String[] args) {
    logger.info("Starting Kafka Consumer...");
    GameEventConsumer gameEventConsumer = gameEventConsumer();
    Thread consumer = new Thread(gameEventConsumer::startConsuming);
    consumer.start();

    Runtime.getRuntime().addShutdownHook(new Thread(gameEventConsumer::close));

    try {
      consumer.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  static GameEventConsumer gameEventConsumer() {
    return new GameEventConsumer(calculator);
  }
}
