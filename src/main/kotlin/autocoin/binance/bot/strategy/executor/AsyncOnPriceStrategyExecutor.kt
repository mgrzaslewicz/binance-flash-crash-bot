package autocoin.binance.bot.strategy.executor

import com.autocoin.exchangegateway.spi.exchange.price.CurrencyPairWithPrice
import java.util.concurrent.ExecutorService

class AsyncOnPriceStrategyExecutor(
    private val decorated: StrategyExecutor,
    private val executorService: ExecutorService,
) : StrategyExecutor by decorated {
    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        executorService.submit {
            decorated.onPriceUpdated(currencyPairWithPrice)
        }
    }
}

fun StrategyExecutor.asyncOnPrice(executorService: ExecutorService): StrategyExecutor {
    return AsyncOnPriceStrategyExecutor(this, executorService)
}
