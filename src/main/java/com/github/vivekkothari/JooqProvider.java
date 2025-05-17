package com.github.vivekkothari;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public class JooqProvider {
  private static final DSLContext dslContext = init();

  private static DSLContext init() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:postgresql://localhost:5432/test");
    config.setUsername("user");
    config.setPassword("password");

    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setIdleTimeout(30000);
    config.setConnectionTimeout(2000);
    config.setPoolName("JooqHikariPool");

    DataSource dataSource = new HikariDataSource(config);
    return DSL.using(dataSource, SQLDialect.POSTGRES);
  }

  public static DSLContext getDsl() {
    return dslContext;
  }
}
