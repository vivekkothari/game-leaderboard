package com.github.vivekkothari;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

public class TopScoreCalculator {

  private static final String SCRIPT =
      """
    local existing = redis.call("ZSCORE", KEYS[1], ARGV[1])
    if not existing or tonumber(ARGV[2]) > tonumber(existing) then
        redis.call("ZADD", KEYS[1], ARGV[2], ARGV[1])
    end
    redis.call("ZREMRANGEBYRANK", KEYS[1], 0, -101)
    return true
    """;

  private final GameDao dao;
  private final Jedis jedis;

  public TopScoreCalculator(GameDao dao, Jedis jedis) {
    this.dao = dao;
    this.jedis = jedis;
  }

  public void insertGame(List<GameService.Game> games) {
    dao.insertBatch(games);
    for (GameService.Game g : games) {
      jedis.eval(
          SCRIPT,
          List.of("top100"),
          List.of(String.valueOf(g.userId()), String.valueOf(g.score())));
    }
  }

  public Map<Integer, Double> getTopUserMaxScores(int limit) {
    List<Tuple> topScores = jedis.zrevrangeWithScores("top100", 0, limit);
    return topScores.stream()
        .collect(
            Collectors.toMap(
                t -> Integer.valueOf(t.getElement()), // convert userId string to Integer
                Tuple::getScore,
                (_, b) -> b, // in case of duplicates (shouldn't happen)
                LinkedHashMap::new // preserve order (highest to lowest)
                ));
  }
}
