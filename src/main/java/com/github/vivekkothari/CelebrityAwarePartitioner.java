package com.github.vivekkothari;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;

/**
 * A custom Kafka partitioner that routes messages from celebrity users to virtual partitions. This
 * ensures that messages from celebrities are distributed across multiple partitions, while
 * non-celebrity users are routed to a single partition based on their user ID.
 */
public class CelebrityAwarePartitioner implements Partitioner {

  // A set of celebrity user IDs that will be routed to virtual partitions.
  // In a real application, this could be fetched from a redis.
  private static final Set<String> CELEBRITIES = Set.of("u-1", "u-10", "u-50", "u-100");

  @Override
  public int partition(
      String topic,
      Object keyObj,
      byte[] keyBytes,
      Object value,
      byte[] valueBytes,
      Cluster cluster) {
    String key = (String) keyObj;
    List<PartitionInfo> partitions = cluster.availablePartitionsForTopic(topic);
    int numPartitions = partitions.size();

    if (CELEBRITIES.contains(key)) {
      int virtualKey = ThreadLocalRandom.current().nextInt(10); // 10 virtual partitions
      return Math.abs((key + virtualKey).hashCode()) % numPartitions;
    }

    return Math.abs(key.hashCode()) % numPartitions;
  }

  @Override
  public void close() {}

  @Override
  public void configure(Map<String, ?> configs) {}
}
