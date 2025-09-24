#!/bin/bash

JAR_NAME="binance-1.0.0.jar"
CONFIG_FILE="./application-prod.properties"
PROFILE="prod"
SESSION_NAME="binance-ws"

echo "Starting Spring Boot application with profile: $PROFILE"
echo "Using config: $CONFIG_FILE"

if [ ! -f "$JAR_NAME" ]; then
  echo "JAR file $JAR_NAME not found"
  exit 1
fi

if tmux has-session -t "$SESSION_NAME" 2>/dev/null; then
  echo "Stopping existing tmux session: $SESSION_NAME"
  tmux kill-session -t "$SESSION_NAME"
fi

tmux new-session -d -s "$SESSION_NAME" \
"java -Dspring.profiles.active=$PROFILE \
     -Dspring.config.location=file:$CONFIG_FILE \
     -jar $JAR_NAME"

echo "Application started in tmux session: $SESSION_NAME"
echo "Attach with: tmux attach -t $SESSION_NAME"
