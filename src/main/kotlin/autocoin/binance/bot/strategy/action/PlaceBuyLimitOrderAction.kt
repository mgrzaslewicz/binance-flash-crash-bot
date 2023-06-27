package autocoin.binance.bot.strategy.action

import mu.KLogging
import java.math.BigDecimal

class PlaceBuyLimitOrderAction(
    val price: BigDecimal,
    val amount: BigDecimal,
) : StrategyAction {
    companion object : KLogging()

    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        return if (strategyExecutor is PlaceBuyLimitOrderActionExecutor) {
            try {
                strategyExecutor.placeBuyLimitOrder(buyPrice = price, baseCurrencyAmount = amount)
                true
            } catch (e: Exception) {
                logger.error(e) { "Error while placing buy limit order" }
                false
            }
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement PlaceBuyLimitOrderActionExecutor")
        }
    }
}

class TryPlaceBuyLimitOrderAction(
    val price: BigDecimal,
    val amount: BigDecimal,
) : StrategyAction {
    companion object : KLogging()

    override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
        if (strategyExecutor is PlaceBuyLimitOrderActionExecutor) {
            try {
                strategyExecutor.placeBuyLimitOrder(buyPrice = price, baseCurrencyAmount = amount)
            } catch (e: Exception) {
                logger.error(e) { "Error while placing buy limit order" }
            }
            return true
        } else {
            throw IllegalArgumentException("StrategyExecutor must implement PlaceBuyLimitOrderActionExecutor")
        }
    }
}
