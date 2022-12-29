package autocoin.binance.bot.strategy

import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.logger.withMinDelayBetweenLogs
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import mu.KLogging
import java.time.Duration

class LoggingStrategyExecutorService(
    private val decorated: StrategyExecutorService,
    minDelayBetweenLogs: Duration = Duration.ofMillis(1),
) : StrategyExecutorService by decorated {
    companion object : KLogging()

    private val loggerWithMinDelayBetweenLogs = logger.withMinDelayBetweenLogs(minDelayBetweenLogs)

    override fun addOrResumeStrategyExecutors(strategyParametersList: Collection<StrategyParametersDto>) {
        logger.info { "Adding or resuming following ${strategyParametersList.size} strategies: $strategyParametersList" }
        decorated.addOrResumeStrategyExecutors(strategyParametersList)
    }

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        loggerWithMinDelayBetweenLogs.info { "Price updated: $currencyPairWithPrice" }
        decorated.onPriceUpdated(currencyPairWithPrice)
    }
}

fun StrategyExecutorService.loggingStrategyExecutor(minDelayBetweenLogs: Duration = Duration.ofMillis(1)): StrategyExecutorService =
    LoggingStrategyExecutorService(this, minDelayBetweenLogs = minDelayBetweenLogs)
