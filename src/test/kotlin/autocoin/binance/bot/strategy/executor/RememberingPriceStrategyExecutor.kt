package autocoin.binance.bot.strategy.executor

import com.autocoin.exchangegateway.spi.exchange.price.CurrencyPairWithPrice
import java.math.BigDecimal

class RememberingPriceStrategyExecutor(private val decorated: StrategyExecutor) : StrategyExecutor by decorated {
    private val prices = mutableListOf<BigDecimal>()
    val lastPrices: List<BigDecimal> get() = prices.toList()

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        decorated.onPriceUpdated(currencyPairWithPrice)
        prices += currencyPairWithPrice.price
    }
}

fun StrategyExecutor.rememberingPrice() = RememberingPriceStrategyExecutor(this)
