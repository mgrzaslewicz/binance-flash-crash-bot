package autocoin.binance.bot.exchange

import com.autocoin.exchangegateway.spi.exchange.price.CurrencyPairWithPrice
import com.autocoin.exchangegateway.spi.exchange.price.PriceListener
import mu.KLogging


class SkippingTooFastProducerPriceListener(
    private val decorated: PriceListener,
    private val onPriceSkipped: (CurrencyPairWithPrice) -> Unit = {},
    private val skippingConsumer: SkippingConsumer,
) : PriceListener {
    private companion object : KLogging()

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        skippingConsumer.run(
            runnable = { decorated.onPriceUpdated(currencyPairWithPrice) },
            onSkipped = {
                onPriceSkipped(currencyPairWithPrice)
            },
        )
    }

}

fun PriceListener.skippingTooFastProducer(
    onPriceSkipped: (CurrencyPairWithPrice) -> Unit = {},
    skippingConsumer: SkippingConsumer = SkippingTooFastProducer(),
): PriceListener {
    return SkippingTooFastProducerPriceListener(this, onPriceSkipped, skippingConsumer)
}
