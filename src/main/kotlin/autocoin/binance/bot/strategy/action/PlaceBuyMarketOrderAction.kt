package autocoin.binance.bot.strategy.action

import mu.KLogging
import java.math.BigDecimal

class PlaceBuyMarketOrderAction(
    val counterCurrencyAmount: BigDecimal,
    val currentPrice: BigDecimal,
) : StrategyAction {
    companion object : KLogging()

    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        return if (strategyExecutor is PlaceBuyMarketOrderActionExecutor) {
            try {
                strategyExecutor.placeBuyMarketOrder(
                    currentPrice = currentPrice,
                    counterCurrencyAmount = counterCurrencyAmount,
                )
                true
            } catch (e: Exception) {
                logger.error(e) { "Error while placing buy market order" }
                false
            }
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement PlaceBuyMarketOrderActionExecutor")
        }
    }
}
