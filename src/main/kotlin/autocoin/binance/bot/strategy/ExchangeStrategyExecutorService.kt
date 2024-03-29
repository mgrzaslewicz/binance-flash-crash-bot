package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto.Companion.matchesStrategyExecution
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto.Companion.toResumedStrategyExecution
import autocoin.binance.bot.strategy.execution.repository.FileBackedMutableSet
import autocoin.binance.bot.strategy.executor.StrategyExecutor
import autocoin.binance.bot.strategy.executor.StrategyExecutorProvider
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.price.CurrencyPairWithPrice

class ExchangeStrategyExecutorService(
    private val strategyExecutions: FileBackedMutableSet<StrategyExecutionDto>,
    private val strategyExecutorProvider: StrategyExecutorProvider,
) : StrategyExecutorService {

    private val runningStrategies = mutableMapOf<CurrencyPair, MutableList<StrategyExecutor>>()
    override fun getRunningStrategies() = runningStrategies.flatMap { it.value }.map { it.strategyExecution }

    override fun addStrategyExecutor(strategyParameters: StrategyParametersDto) {
        val strategiesRunningWithCurrencyPair =
            runningStrategies.getOrPut(strategyParameters.currencyPair) { ArrayList() }
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

    override fun currencyPairsOfRunningStrategies(): Set<CurrencyPair> {
        return runningStrategies.keys.toSet()
    }

    override fun addOrResumeStrategyExecutors(strategyParametersList: Collection<StrategyParametersDto>) {
        val strategiesToResume = strategyExecutions.filter { execution ->
            strategyParametersList.any { parameters ->
                parameters.matchesStrategyExecution(execution)
            }
        }
        val strategiesToDelete = strategyExecutions.filter { execution ->
            strategyParametersList.any { parameters ->
                !parameters.matchesStrategyExecution(execution)
            }
        }
        strategyExecutions.removeAll(strategiesToDelete.toSet())
        strategyExecutions.addAll(strategiesToResume)
        strategyExecutions.save()

        val strategyExecutionsToResume =
            strategiesToResume.map { execution ->
                val matchingParameters =
                    strategyParametersList.find { parameters -> parameters.matchesStrategyExecution(execution) }!!
                matchingParameters.toResumedStrategyExecution(strategyExecution = execution)
            }
        strategyExecutionsToResume.forEach {
            val strategiesRunningWithCurrencyPair = runningStrategies.getOrPut(it.currencyPair) { ArrayList() }
            val strategyExecutor = strategyExecutorProvider.createStrategyExecutor(it)
            strategiesRunningWithCurrencyPair.add(strategyExecutor)
        }

        val strategiesToAdd = strategyParametersList.filter {
            strategyExecutions.none { execution -> it.matchesStrategyExecution(execution) }
        }
        strategiesToAdd.forEach {
            addStrategyExecutor(it)
        }
    }
}
