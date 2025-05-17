package com.github.vivekkothari;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.annotation.Default;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.annotation.Post;
import java.time.Instant;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameService {

  private static final Random random = new Random();
  private static final Logger logger = LoggerFactory.getLogger(GameService.class);

  public GameService(GameEventProducer producer, TopScoreCalculator calculator) {
    this.producer = producer;
    this.calculator = calculator;
  }

  public record Game(int id, int userId, long score, Instant attainedAt) {
    Game(int userId, long score, Instant attainedAt) {
      this(random.nextInt(0, Integer.MAX_VALUE), userId, score, attainedAt);
    }
  }

  private final GameEventProducer producer;
  private final TopScoreCalculator calculator;

  @Post("/game")
  public void gameComplete(Game game) {
    logger.info(game.toString());
    producer.publishScore(game);
  }

  @Get("/top-scores")
  public HttpResponse getTopScores(@Param("limit") @Default("10") int limit) {
    return HttpResponse.ofJson(calculator.getTopUserTotalScores(limit));
  }
}
