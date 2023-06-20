package autocoin.binance.bot.strategy.action

import java.math.BigDecimal

class PlaceBuyMarketOrderAction(
    val counterCurrencyAmount: BigDecimal,
    val currentPrice: BigDecimal,
    override val shouldBreakActionChainOnFail: Boolean
) : StrategyAction {
    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        if (strategyExecutor is PlaceBuyMarketOrderActionExecutor) {
            return strategyExecutor.placeBuyMarketOrder(
                currentPrice = currentPrice,
                counterCurrencyAmount = counterCurrencyAmount,
            ) != null
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement PlaceBuyMarketOrderActionExecutor")
        }
    }
}
