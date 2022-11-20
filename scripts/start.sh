#!/bin/bash
MAKE_REAL_ORDERS=true java -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -jar ../target/autocoin-binance-bot-1.0-SNAPSHOT.jar &
