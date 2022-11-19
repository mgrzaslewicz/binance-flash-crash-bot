package autocoin.binance.bot.strategy.action

import autocoin.binance.bot.strategy.executor.StrategyExecutor

interface StrategyAction {
    fun apply(strategyExecutor: StrategyExecutor): Boolean
    val shouldBreakActionChainOnFail: Boolean
}
