package autocoin.binance.bot.strategy.action

import autocoin.binance.bot.strategy.executor.StrategyExecutor
import java.math.BigDecimal

class PlaceBuyMarketOrderAction(
    val counterCurrencyAmount: BigDecimal,
    val currentPrice: BigDecimal,
    override val shouldBreakActionChainOnFail: Boolean
) : StrategyAction {
    override fun apply(strategyExecutor: StrategyExecutor): Boolean {
        val result = strategyExecutor.placeBuyMarketOrder(
            currentPrice = currentPrice,
            counterCurrencyAmount = counterCurrencyAmount
        ) != null
        return result
    }
}
