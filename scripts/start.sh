#!/bin/bash
MAKE_REAL_ORDERS=true java -Xmx128M -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -jar autocoin-binance-bot-1.0-SNAPSHOT.jar &
