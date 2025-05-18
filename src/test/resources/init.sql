CREATE TABLE game (
    id          SERIAL PRIMARY KEY,
    user_id     TEXT          NOT NULL,
    score       BIGINT       NOT NULL,
    attained_at TIMESTAMPTZ  NOT NULL
);