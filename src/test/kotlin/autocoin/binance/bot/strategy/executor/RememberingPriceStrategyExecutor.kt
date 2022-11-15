package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import java.math.BigDecimal

class RememberingPriceStrategyExecutor(private val decorated: StrategyExecutor) : StrategyExecutor by decorated {
    val lastPrices: MutableList<BigDecimal> = mutableListOf()
    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        decorated.onPriceUpdated(currencyPairWithPrice)
        lastPrices += currencyPairWithPrice.price
    }
}

fun StrategyExecutor.rememberingPrice() = RememberingPriceStrategyExecutor(this)
