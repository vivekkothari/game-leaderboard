package com.github.vivekkothari;

import com.github.vivekkothari.redis.RedisClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import redis.clients.jedis.resps.Tuple;

public class TopScoreCalculator {

  private static final String MAX_SCORE_SCRIPT =
      """
    local existing = redis.call("ZSCORE", KEYS[1], ARGV[1])
    if not existing or tonumber(ARGV[2]) > tonumber(existing) then
        redis.call("ZADD", KEYS[1], ARGV[2], ARGV[1])
    end
    redis.call("ZREMRANGEBYRANK", KEYS[1], 0, -101)
    return true
    """;

  private static final String TOTAL_SCORE_SCRIPT =
      """
    for i = 1, #ARGV, 2 do
      local userId = ARGV[i]
      local scoreIncrement = ARGV[i + 1]
      redis.call("ZINCRBY", KEYS[1], scoreIncrement, userId)
    end
    return true
  """;

  private final RedisClient redisClient;

  public TopScoreCalculator(RedisClient redisClient) {
    this.redisClient = redisClient;
  }

  public void insertGame(List<GameService.Game> games) {
    for (GameService.Game g : games) {
      redisClient.use(
          jedis ->
              jedis.eval(
                  MAX_SCORE_SCRIPT,
                  List.of("top100"),
                  List.of(g.userId(), String.valueOf(g.score()))));
    }
    // Build ARGV: userId1, score1, userId2, score2, ...
    List<String> args = new ArrayList<>();
    for (GameService.Game g : games) {
      args.add(g.userId());
      args.add(String.valueOf(g.score()));
    }
    redisClient.use(jedis -> jedis.eval(TOTAL_SCORE_SCRIPT, List.of("totalTop100"), args));
  }

  public Map<String, Double> getTopUserMaxScores(int limit) {
    List<Tuple> topScores =
        redisClient.with(jedis -> jedis.zrevrangeWithScores("top100", 0, limit));
    return topScores.stream()
        .collect(
            Collectors.toMap(
                Tuple::getElement, // convert userId string to Integer
                Tuple::getScore,
                (_, b) -> b, // in case of duplicates (shouldn't happen)
                LinkedHashMap::new // preserve order (highest to lowest)
                ));
  }

  public Map<String, Double> getTopUserTotalScores(int limit) {
    List<Tuple> topScores =
        redisClient.with(jedis -> jedis.zrevrangeWithScores("totalTop100", 0, limit));
    return topScores.stream()
        .collect(
            Collectors.toMap(
                Tuple::getElement, // convert userId string to Integer
                Tuple::getScore,
                (_, b) -> b, // in case of duplicates (shouldn't happen)
                LinkedHashMap::new // preserve order (highest to lowest)
                ));
  }
}
