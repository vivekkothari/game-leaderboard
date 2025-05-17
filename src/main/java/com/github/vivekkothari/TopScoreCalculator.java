package com.github.vivekkothari;

import java.util.Map;

public class TopScoreCalculator {

  private final GameDao dao;

  public TopScoreCalculator(GameDao dao) {
    this.dao = dao;
  }

  public Map<Integer, Long> getTopUserTotalScores(int limit) {
    return dao.getTopUserTotalScores(limit);
  }
}
