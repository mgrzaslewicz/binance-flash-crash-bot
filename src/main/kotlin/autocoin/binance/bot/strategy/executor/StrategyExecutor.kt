package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.PriceListener
import autocoin.binance.bot.strategy.execution.StrategyExecution

interface StrategyExecutor : PriceListener {
    val strategyExecution: StrategyExecution
}
