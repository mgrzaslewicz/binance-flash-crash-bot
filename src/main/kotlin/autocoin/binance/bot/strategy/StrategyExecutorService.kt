package autocoin.binance.bot.strategy

import autocoin.binance.bot.app.config.ExchangeName
import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.exchange.PriceListener
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
import autocoin.binance.bot.strategy.executor.StrategyExecutor
import autocoin.binance.bot.strategy.executor.StrategyExecutorProvider
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import mu.KLogging
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

data class ExchangeApiKey(
    val apiKey: String,
    val secretKey: String,
) {
    override fun toString(): String {
        return "ExchangeApiKey(apiKey='${apiKey.substring(0, 4)}', secretKey='${secretKey.substring(0, 4)}')"
    }
}

data class StrategyParameters(
    val currencyPair: CurrencyPair,
    val userId: String,
    val counterCurrencyAmountLimitForBuying: BigDecimal,
    val numberOfBuyLimitOrdersToKeep: Int = 4,
    val exchangeApiKey: ExchangeKeyDto,
) {
    fun toStrategyExecution(): StrategyExecution {
        return StrategyExecution(
            exchangeName = ExchangeName.BINANCE,
            userId = userId,
            baseCurrencyCode = currencyPair.base,
            counterCurrencyCode = currencyPair.counter,
            counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
            createTimeMillis = ZonedDateTime.now().toInstant().toEpochMilli(),
            exchangeApiKey = exchangeApiKey,
        )
    }
}

interface StrategyExecutorService : PriceListener {
    fun addStrategyExecutor(strategyParameters: StrategyParameters)
}

class DefaultStrategyExecutorService(
    private val strategyExecutionRepository: StrategyExecutionRepository,
    private val strategyExecutorProvider: StrategyExecutorProvider
) : StrategyExecutorService {

    private val runningStrategies = mutableMapOf<CurrencyPair, MutableList<StrategyExecutor>>()
    override fun addStrategyExecutor(strategyParameters: StrategyParameters) {
        val strategiesRunningWithCurrencyPair = runningStrategies.getOrPut(strategyParameters.currencyPair) { ArrayList() }
        if (strategiesRunningWithCurrencyPair.any { it.strategyExecution.userId == strategyParameters.userId }) {
            throw RuntimeException("User ${strategyParameters.userId} already has strategy running on currency pair ${strategyParameters.currencyPair}")
        }
        val strategyExecutor = strategyExecutorProvider.createStrategyExecutor(strategyParameters)
        strategiesRunningWithCurrencyPair.add(strategyExecutor)
    }

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        runningStrategies[currencyPairWithPrice.currencyPair]
            ?.forEach { it.onPriceUpdated(currencyPairWithPrice) }
    }

}

class LoggingStrategyExecutorService(
    private val decorated: StrategyExecutorService,
    private val minDelayBetweenLogs: Duration = Duration.ofMillis(1),
    private val clock: Clock
) :
    StrategyExecutorService by decorated {
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
