package com.github.vivekkothari;

import static com.github.vivekkothari.jooq.generated.Tables.GAME;

import com.github.vivekkothari.jooq.generated.tables.records.GameRecord;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

public class GameDao {

  private final DSLContext dsl;

  public GameDao(DSLContext dsl) {
    this.dsl = dsl;
  }

  public void insertBatch(List<GameService.Game> games) {
    if (games == null || games.isEmpty()) return;

    List<GameRecord> records =
        games.stream()
            .map(
                game -> {
                  GameRecord record = dsl.newRecord(GAME);
                  record.setUserId(game.userId());
                  record.setScore(game.score());
                  record.setAttainedAt(game.attainedAt().atOffset(ZoneOffset.UTC));
                  return record;
                })
            .toList();

    dsl.batchInsert(records).execute();
  }

  public Map<Integer, Long> getTopUserTotalScores(int limit) {
    return dsl.select(GAME.USER_ID, DSL.sum(GAME.SCORE))
        .from(GAME)
        .groupBy(GAME.USER_ID)
        .orderBy(DSL.sum(GAME.SCORE).desc())
        .limit(limit)
        .fetchStream()
        .collect(
            Collectors.toMap(
                r -> r.get(GAME.USER_ID),
                r -> r.get(DSL.sum(GAME.SCORE), Long.class),
                (a, b) -> a, // merge function (won’t be used since keys are unique)
                LinkedHashMap::new // preserve insertion order (descending score)
                ));
  }
}
