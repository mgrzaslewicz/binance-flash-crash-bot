package autocoin.binance.bot.strategy.action

import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.executor.StrategyExecutor

class CancelOrderAction(val strategyOrder: StrategyOrder, override val shouldBreakActionChainOnFail: Boolean) : StrategyAction {
    override fun apply(strategyExecutor: StrategyExecutor): Boolean {
        return strategyExecutor.cancelOrder(order = strategyOrder)
    }
}
