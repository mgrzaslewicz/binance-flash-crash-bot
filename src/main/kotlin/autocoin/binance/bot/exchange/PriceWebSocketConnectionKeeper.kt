package autocoin.binance.bot.exchange

import mu.KLogging
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class PriceWebSocketConnectionKeeper(
    private val clock: Clock,
    private val binancePriceStream: BinancePriceStream,
    private val scheduledExecutor: ScheduledExecutorService,
    private val maxDurationWithoutUpdate: Duration = Duration.ofSeconds(10),
) : PriceListener {
    private companion object : KLogging()

    private var lastUpdate: Instant = clock.instant()

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        lastUpdate = clock.instant()
    }

    fun scheduleCheckingConnection() {
        scheduledExecutor.scheduleWithFixedDelay({ reconnectWhenNoUpdatesForTooLong() }, maxDurationWithoutUpdate.seconds * 2, maxDurationWithoutUpdate.seconds, TimeUnit.SECONDS)
    }

    private fun reconnectWhenNoUpdatesForTooLong() {
        if (Duration.between(lastUpdate, clock.instant()) > maxDurationWithoutUpdate) {
            logger.warn { "No price updates for $maxDurationWithoutUpdate, reconnecting" }
            binancePriceStream.reconnect()
        }
    }

}
