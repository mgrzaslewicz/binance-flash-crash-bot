package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.SkippingConsumer
import autocoin.binance.bot.exchange.SkippingTooFastProducer
import com.autocoin.exchangegateway.spi.exchange.price.CurrencyPairWithPrice
import mu.KLogging

class SkippingTooFastProducerStrategyExecutor(
    private val decorated: StrategyExecutor,
    private val skippingConsumer: SkippingConsumer,
) : StrategyExecutor by decorated {
    private companion object : KLogging()

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        skippingConsumer.run(
            runnable = { decorated.onPriceUpdated(currencyPairWithPrice) },
            onSkipped = {
                val logTag =
                    "user=${strategyExecution.userId}, currencyPair=${strategyExecution.currencyPair}, strategyType=${strategyExecution.strategyType}"
                logger.info { "[$logTag] Previous actions have not finished yet. Skipping this price update: $currencyPairWithPrice" }
            },
        )
    }
}

fun StrategyExecutor.skippingTooFastProducer(
    skippingConsumer: SkippingConsumer = SkippingTooFastProducer(),
): StrategyExecutor {
    return SkippingTooFastProducerStrategyExecutor(this, skippingConsumer)
}
