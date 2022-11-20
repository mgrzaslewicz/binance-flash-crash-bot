#!/bin/bash
PID=$(pgrep -f "autocoin-binance-bot.*.jar")
kill -9 "$PID"
