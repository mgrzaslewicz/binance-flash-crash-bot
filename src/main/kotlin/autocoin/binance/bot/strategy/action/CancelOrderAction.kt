package autocoin.binance.bot.strategy.action

import autocoin.binance.bot.strategy.execution.repository.StrategyOrder

class CancelOrderAction(val strategyOrder: StrategyOrder, override val shouldBreakActionChainOnFail: Boolean) :
    StrategyAction {
    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        if (strategyExecutor is CancelOrderActionExecutor) {
            return strategyExecutor.cancelOrder(order = strategyOrder)
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement CancelOrderActionExecutor")
        }
    }
}
