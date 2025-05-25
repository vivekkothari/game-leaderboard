package com.github.vivekkothari;

import static com.github.vivekkothari.GameEventProducer.BOOTSTRAP_SERVERS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.linecorp.armeria.internal.common.JacksonUtil;
import com.linecorp.armeria.internal.shaded.guava.collect.ImmutableMap;
import java.time.Duration;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameCdcConsumer implements AutoCloseable {

  private static final ObjectMapper objectMapper =
      JacksonUtil.newDefaultObjectMapper()
          .copy()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

  private static final Logger logger = LoggerFactory.getLogger(GameCdcConsumer.class);

  private final KafkaConsumer<String, String> consumer =
      new KafkaConsumer<>(
          ImmutableMap.<String, Object>builder()
              .put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false)
              .put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
              .put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, ConsumerConfig.DEFAULT_MAX_POLL_RECORDS)
              .put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, (int) 1e6) // 1MB
              .put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, (int) 1e7) // 10MB
              .put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 50)
              .put(ConsumerConfig.GROUP_ID_CONFIG, "game-cdc-group")
              .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName())
              .put(
                  ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                  StringDeserializer.class.getName())
              .put("json.serde.target.class", GameService.Game.class.getName())
              .put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
              .put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
              .build());
  private final TopScoreCalculator calculator;

  private volatile boolean closed = false;

  public GameCdcConsumer(TopScoreCalculator calculator) {
    this.calculator = calculator;
  }

  public void startConsuming() {
    consumer.subscribe(List.of("test.public.game"));
    while (!closed) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
      if (!records.isEmpty()) {
        logger.info("Received {} CDC records", records.count());
        records.forEach(
            record -> {
              try {
                Payload<GameService.Game> event =
                    objectMapper.readValue(record.value(), new TypeReference<>() {});
                logger.info("Processing CDC event: {}", event);
                calculator.insertGame(List.of(event.after()));
                consumer.commitSync();
              } catch (JsonProcessingException e) {
                logger.error("Error processing CDC event", e);
              }
            });
      }
    }
  }

  @Override
  public void close() {
    closed = true;
    consumer.close();
  }

  record DebeziumChangeEvent<T>(Payload<T> payload) {}

  record Payload<T>(String op, T before, T after, DebeziumSource source) {}

  record DebeziumSource(String db, String schema, String table) {}
}

/*
kafka-consumer-groups \
  --bootstrap-server kafka-broker-1:9092 \
  --group game-cdc-group \
  --topic test.public.game \
  --reset-offsets --to-earliest --execute

 */
