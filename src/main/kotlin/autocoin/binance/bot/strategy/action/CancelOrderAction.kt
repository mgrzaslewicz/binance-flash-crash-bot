package autocoin.binance.bot.strategy.action

import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import mu.KLogging

class CancelOrderAction(val strategyOrder: StrategyOrder) : StrategyAction {
    private companion object : KLogging()

    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        if (strategyExecutor is CancelOrderActionExecutor) {
            return try {
                strategyExecutor.cancelOrder(order = strategyOrder)
            } catch (e: Exception) {
                logger.error(e) { "Error while cancelling order" }
                false
            }
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement CancelOrderActionExecutor")
        }
    }
}
