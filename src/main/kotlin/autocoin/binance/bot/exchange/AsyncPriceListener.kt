package autocoin.binance.bot.exchange

import com.autocoin.exchangegateway.spi.exchange.price.CurrencyPairWithPrice
import com.autocoin.exchangegateway.spi.exchange.price.PriceListener
import mu.KLogging
import java.util.concurrent.ExecutorService

class AsyncPriceListener(
    private val executorService: ExecutorService,
    private val decorated: PriceListener,
) : PriceListener {
    private companion object : KLogging()

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        executorService.submit {
            decorated.onPriceUpdated(currencyPairWithPrice)
        }
    }

}

fun PriceListener.async(executorService: ExecutorService): PriceListener {
    return AsyncPriceListener(executorService, this)
}


