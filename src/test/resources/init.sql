CREATE TABLE game (
    id          SERIAL PRIMARY KEY,
    user_id     INT          NOT NULL,
    score       BIGINT       NOT NULL,
    attained_at TIMESTAMPTZ  NOT NULL
);