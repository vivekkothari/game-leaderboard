#!/bin/bash

# Usage: ./simulate_game_play.sh <num_requests> <userId_from> <userId_to>

NUM_REQUESTS=$1
USERID_FROM=$2
USERID_TO=$3

if [ -z "$NUM_REQUESTS" ] || [ -z "$USERID_FROM" ] || [ -z "$USERID_TO" ]; then
  echo "Usage: $0 <num_requests> <userId_from> <userId_to>"
  exit 1
fi

# Define celeb weights manually (in parallel arrays)
CELEB_IDS=("u-6" "u-8" "u-10")    # Add your celeb user IDs here
CELEB_WEIGHTS=(5 3 10)           # Corresponding weights

# Build the weighted user list
WEIGHTED_USERIDS=()

for id in $(seq "$USERID_FROM" "$USERID_TO"); do
  user_id="u-$id"
  weight=1

  for i in "${!CELEB_IDS[@]}"; do
    if [ "${CELEB_IDS[$i]}" = "$user_id" ]; then
      weight=${CELEB_WEIGHTS[$i]}
      break
    fi
  done

  for ((w=0; w<weight; w++)); do
    WEIGHTED_USERIDS+=("$user_id")
  done
done

if [ ${#WEIGHTED_USERIDS[@]} -eq 0 ]; then
  echo "Error: weighted user list is empty!"
  exit 1
fi

# Run all requests in parallel
for ((i=1; i<=NUM_REQUESTS; i++)); do
  idx=$(( RANDOM % ${#WEIGHTED_USERIDS[@]} ))
  user_id=${WEIGHTED_USERIDS[$idx]}
  score=$(( RANDOM % 100 + 1 ))
  attained_at=$(date +%s.%N)

  json_payload=$(jq -n \
    --arg userId "$user_id" \
    --argjson score "$score" \
    --arg attainedAt "$attained_at" \
    '{userId: $userId, score: $score, attainedAt: ($attainedAt | tonumber)}')

  curl -s -XPOST -H 'content-type: application/json; charset=utf-8' \
    'http://localhost:4040/game' \
    -d "$json_payload" &

  echo "  -> Fired request #$i: userId=$user_id, score=$score, attainedAt=$attained_at"
done

wait