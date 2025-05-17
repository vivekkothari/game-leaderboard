# Game Leaderboard

## Description

An attempt to implement a popular design question asked in various interviews.

## Getting started

```
git clone https://github.com/vivekkothari/game-leaderboard.git
cd game-leaderboard
docker compose up -d
```

Then run src/main/java/com/github/vivekkothari/Main.java file

This will start,

1. server accepting request of game completion.
2. a kafka producer
3. a kafka consumer

## Simulate game completion events

You can simulate game completion events by running the following command in a separate terminal:

```bash
./simulate_game_play.sh 10000 1 100   
```

the first arg is num of events, second is from userId, last is to userId.

## View top k players

You can view the top k players by running the following command in a separate terminal:

```
curl -X GET "http://localhost:4040/top-scores?limit=10"
```
