package com.github.vivekkothari;

import com.linecorp.armeria.internal.shaded.guava.collect.ImmutableMap;
import java.util.UUID;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameEventProducer implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(GameEventProducer.class);

  static final String BOOTSTRAP_SERVERS = "localhost:9092";

  private final KafkaProducer<String, GameService.Game> producer =
      new KafkaProducer<>(
          ImmutableMap.<String, Object>builder()
              .put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
              .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
              .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonRecordSerde.class)
              .put("json.serde.target.class", GameService.Game.class.getName())
              .put(ProducerConfig.ACKS_CONFIG, "all")
              .put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "5000")
              .build());

  static final String TOPIC = "leaderboard";

  public void publishScore(GameService.Game game) {
    producer.send(
        new ProducerRecord<>(TOPIC, UUID.randomUUID().toString(), game),
        (metadata, exception) -> {
          if (exception != null) {
            logger.error("Error sending message", exception);
          } else {
            logger.info("Message was accepted by kafka {}", metadata);
          }
        });
  }

  @Override
  public void close() {
    producer.close();
  }
}
