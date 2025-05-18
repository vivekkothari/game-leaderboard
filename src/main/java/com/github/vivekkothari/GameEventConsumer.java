package com.github.vivekkothari;

import static com.github.vivekkothari.GameEventProducer.BOOTSTRAP_SERVERS;
import static com.github.vivekkothari.GameEventProducer.TOPIC;

import com.linecorp.armeria.internal.shaded.guava.collect.ImmutableMap;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameEventConsumer implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(GameEventConsumer.class);

  private final KafkaConsumer<String, GameService.Game> consumer =
      new KafkaConsumer<>(
          ImmutableMap.<String, Object>builder()
              .put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
              .put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500)
              .put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, (int) 1e6) // 1MB
              .put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 50)
              .put(ConsumerConfig.GROUP_ID_CONFIG, "leaderboard-consumer-group")
              .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName())
              .put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonRecordSerde.class.getName())
              .put("json.serde.target.class", GameService.Game.class.getName())
              .put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
              .put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
              .build());
  private final TopScoreCalculator calculator;

  private volatile boolean closed = false;

  public GameEventConsumer(TopScoreCalculator calculator) {
    this.calculator = calculator;
  }

  public void startConsuming() {
    consumer.subscribe(List.of(TOPIC));
    while (!closed) {
      var records = consumer.poll(Duration.ofMillis(100));
      if (records.count() > 0) {
        logger.info("Received {} records", records.count());
        calculator.insertGame(
            StreamSupport.stream(records.spliterator(), false).map(ConsumerRecord::value).toList());
      }
      consumer.commitAsync();
    }
  }

  @Override
  public void close() {
    closed = true;
    consumer.close();
  }
}
