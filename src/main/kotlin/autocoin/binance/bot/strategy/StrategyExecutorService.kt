package autocoin.binance.bot.strategy

import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.exchange.PriceListener
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.exchange.currency.CurrencyPair
import mu.KLogging
import java.time.Clock
import java.time.Duration
import java.time.Instant


interface StrategyExecutorService : PriceListener {
    fun addStrategyExecutor(strategyParameters: StrategyParameters)
    fun currencyPairsCurrentlyNeeded(): List<CurrencyPair>
    fun addOrResumeStrategyExecutors(strategyParametersList: List<StrategyParameters>)
}

class LoggingStrategyExecutorService(
    private val decorated: StrategyExecutorService,
    private val minDelayBetweenLogs: Duration = Duration.ofMillis(1),
    private val clock: Clock
) : StrategyExecutorService by decorated {
    companion object : KLogging()

    private var lastLogInstant: Instant? = null

    override fun addOrResumeStrategyExecutors(strategyParametersList: List<StrategyParameters>) {
        logger.info { "Adding or resuming following strategies: $strategyParametersList" }
        decorated.addOrResumeStrategyExecutors(strategyParametersList)
    }

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        val now = clock.instant()
        if (lastLogInstant == null || Duration.between(lastLogInstant, now) > minDelayBetweenLogs) {
            lastLogInstant = now
            logger.info { "Price updated: $currencyPairWithPrice" }
        }
        decorated.onPriceUpdated(currencyPairWithPrice)
    }
}
