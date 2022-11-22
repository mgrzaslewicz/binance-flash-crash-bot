package autocoin.binance.bot.logger

import mu.KLogger
import java.time.Clock
import java.time.Duration
import java.time.Instant

class MinDelayBetweenLogsLogger(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val decorated: KLogger,
    private val minDelayBetweenLogs: Duration,
) : KLogger by decorated {
    private var lastLogInstant: Instant? = null
    override fun info(msg: () -> Any?) {
        val now = clock.instant()
        if (lastLogInstant == null || Duration.between(lastLogInstant, now) > minDelayBetweenLogs) {
            lastLogInstant = now
            decorated.info(msg)
        }
    }
}

fun KLogger.withMinDelayBetweenLogs(minDelayBetweenLogs: Duration): MinDelayBetweenLogsLogger {
    return MinDelayBetweenLogsLogger(decorated = this, minDelayBetweenLogs = minDelayBetweenLogs)
}
