package autocoin.binance.bot.strategy.action

import java.math.BigDecimal

class PlaceBuyLimitOrderAction(
    val price: BigDecimal,
    val amount: BigDecimal,
    override val shouldBreakActionChainOnFail: Boolean
) : StrategyAction {
    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        if (strategyExecutor is PlaceBuyLimitOrderActionExecutor) {
            val result = strategyExecutor.placeBuyLimitOrder(buyPrice = price, baseCurrencyAmount = amount) != null
            return result
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement PlaceBuyLimitOrderActionExecutor")
        }
    }
}
