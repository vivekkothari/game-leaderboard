#!/bin/bash

# Usage: ./script.sh <num_requests> <userId_from> <userId_to>

NUM_REQUESTS=$1
USERID_FROM=$2
USERID_TO=$3

if [ -z "$NUM_REQUESTS" ] || [ -z "$USERID_FROM" ] || [ -z "$USERID_TO" ]; then
  echo "Usage: $0 <num_requests> <userId_from> <userId_to>"
  exit 1
fi

for ((i=1; i<=NUM_REQUESTS; i++))
do
  # Generate random userId in range
  USERID=$(( RANDOM % (USERID_TO - USERID_FROM + 1) + USERID_FROM ))

  # Generate random score 1-100
  SCORE=$(( RANDOM % 100 + 1 ))

  # Generate current timestamp with fractional seconds (epoch float)
  ATTAINED_AT=$(date +%s.%N)

  # Compose JSON payload
  JSON_PAYLOAD=$(jq -n \
    --argjson userId "$USERID" \
    --argjson score "$SCORE" \
    --arg attainedAt "$ATTAINED_AT" \
    '{userId: $userId, score: $score, attainedAt: ($attainedAt | tonumber)}')

  # Send POST request
  curl -s -XPOST -H 'content-type: application/json; charset=utf-8' \
    'http://localhost:4040/game' \
    -d "$JSON_PAYLOAD"

  echo "  -> Fired request #$i: userId=$USERID, score=$SCORE, attainedAt=$ATTAINED_AT"
done
