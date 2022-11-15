package autocoin.binance.bot.exchange

import autocoin.binance.bot.strategy.LoggingStrategyExecutorService
import autocoin.binance.bot.strategy.StrategyExecutorService
import mu.KLogging
import java.time.Clock
import java.time.Duration
import java.time.Instant

interface PriceListener {
    fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice)
}

class LoggingPriceListener(
    private val decorated: PriceListener,
    private val minDelayBetweenLogs: Duration = Duration.ofMillis(1),
    private val clock: Clock
) :
    PriceListener by decorated {
    companion object : KLogging()

    private var lastLogInstant: Instant? = null

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        val now = clock.instant()
        if (lastLogInstant == null || Duration.between(lastLogInstant, now) > minDelayBetweenLogs) {
            lastLogInstant = now
            logger.info { "Price updated: $currencyPairWithPrice" }
        }
        decorated.onPriceUpdated(currencyPairWithPrice)
    }
}

fun PriceListener.logging(minDelayBetweenLogs: Duration = Duration.ofMillis(1), clock: Clock): PriceListener = LoggingPriceListener(this, clock = clock)
fun StrategyExecutorService.logging(minDelayBetweenLogs: Duration = Duration.ofMillis(1), clock: Clock): StrategyExecutorService =
    LoggingStrategyExecutorService(this, minDelayBetweenLogs = minDelayBetweenLogs, clock = clock)

