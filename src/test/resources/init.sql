CREATE TABLE game
(
    id          SERIAL PRIMARY KEY,
    user_id     TEXT        NOT NULL,
    score       BIGINT      NOT NULL,
    attained_at TIMESTAMPTZ NOT NULL
);

--By default, Postgres uses a limited form of row-level tracking, and may not log enough details for Debezium to emit full row-level changes.
--Step 1: Enable full row-level replication
ALTER TABLE game
    REPLICA IDENTITY FULL;
--This tells Postgres to log full row contents in the WAL for updates/deletes/inserts, not just the primary key or minimal data.
