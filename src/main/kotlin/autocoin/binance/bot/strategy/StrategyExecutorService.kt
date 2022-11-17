package autocoin.binance.bot.strategy

import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.exchange.PriceListener
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
import autocoin.binance.bot.strategy.executor.StrategyExecutor
import autocoin.binance.bot.strategy.executor.StrategyExecutorProvider
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.exchange.currency.CurrencyPair
import mu.KLogging
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class ExchangeApiKey(
    val apiKey: String,
    val secretKey: String,
) {
    override fun toString(): String {
        return "ExchangeApiKey(apiKey='${apiKey.substring(0, 4)}', secretKey='${secretKey.substring(0, 4)}')"
    }
}

interface StrategyExecutorService : PriceListener {
    fun addStrategyExecutor(strategyParameters: StrategyParameters)
    fun currencyPairsCurrentlyNeeded(): List<CurrencyPair>
    fun addOrResumeStrategyExecutors(strategyParametersList: List<StrategyParameters>)
}

class DefaultStrategyExecutorService(
    private val strategyExecutionRepository: StrategyExecutionRepository,
    private val strategyExecutorProvider: StrategyExecutorProvider
) : StrategyExecutorService {

    private val runningStrategies = mutableMapOf<CurrencyPair, MutableList<StrategyExecutor>>()
    override fun addStrategyExecutor(strategyParameters: StrategyParameters) {
        val strategiesRunningWithCurrencyPair = runningStrategies.getOrPut(strategyParameters.currencyPair) { ArrayList() }
        if (strategiesRunningWithCurrencyPair.any { strategyParameters.matchesStrategyExecution(it.strategyExecution) }) {
            throw RuntimeException("User ${strategyParameters.userId} already has strategy running on currency pair ${strategyParameters.currencyPair}")
        }
        val strategyExecutor = strategyExecutorProvider.createStrategyExecutor(strategyParameters)
        strategiesRunningWithCurrencyPair.add(strategyExecutor)
    }

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        runningStrategies[currencyPairWithPrice.currencyPair]
            ?.forEach { it.onPriceUpdated(currencyPairWithPrice) }
    }

    override fun currencyPairsCurrentlyNeeded(): List<CurrencyPair> {
        return runningStrategies.keys.toList()
    }

    override fun addOrResumeStrategyExecutors(strategyParametersList: List<StrategyParameters>) {
        val allExecutions = strategyExecutionRepository.getExecutions()
        val strategiesToResume = allExecutions.filter { execution ->
            strategyParametersList.any { parameters ->
                parameters.matchesStrategyExecution(execution)
            }
        }
        val strategiesToDelete = allExecutions.filter { execution ->
            strategyParametersList.any { parameters ->
                !parameters.matchesStrategyExecution(execution)
            }
        }
        strategyExecutionRepository.delete(strategiesToDelete)
        strategyExecutionRepository.save(strategiesToResume)

        val strategyExecutionsToResume =
            strategiesToResume.map { execution ->
                val matchingParameters = strategyParametersList.find { parameters -> parameters.matchesStrategyExecution(execution) }!!
                matchingParameters.toResumedStrategyExecution(strategyExecution = execution)
            }
        strategyExecutionsToResume.forEach {
            val strategiesRunningWithCurrencyPair = runningStrategies.getOrPut(it.currencyPair) { ArrayList() }
            val strategyExecutor = strategyExecutorProvider.createStrategyExecutor(it)
            strategiesRunningWithCurrencyPair.add(strategyExecutor)
        }
    }
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
