package com.github.vivekkothari;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.armeria.internal.common.JacksonUtil;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class JsonRecordSerde<T> implements Serializer<T>, Deserializer<T> {

  private final ObjectMapper objectMapper = JacksonUtil.newDefaultObjectMapper();
  private Class<T> targetType;

  @SuppressWarnings("unchecked")
  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    Object className = configs.get("json.serde.target.class");
    if (className instanceof String) {
      try {
        this.targetType = (Class<T>) Class.forName((String) className);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("Class not found for deserialization", e);
      }
    }
  }

  @Override
  public T deserialize(String topic, byte[] data) {
    if (data == null) return null;
    try {
      return objectMapper.readValue(data, targetType);
    } catch (Exception e) {
      throw new RuntimeException("Error deserializing JSON", e);
    }
  }

  @Override
  public byte[] serialize(String topic, T data) {
    if (data == null) return null;
    try {
      return objectMapper.writeValueAsBytes(data);
    } catch (Exception e) {
      throw new RuntimeException("Error serializing JSON", e);
    }
  }

  @Override
  public void close() {}
}
