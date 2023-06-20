package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.price.PriceListener


interface StrategyExecutorService : PriceListener {
    fun addStrategyExecutor(strategyParameters: StrategyParametersDto)
    fun currencyPairsOfRunningStrategies(): Set<CurrencyPair>
    fun addOrResumeStrategyExecutors(strategyParametersList: Collection<StrategyParametersDto>)
    fun getRunningStrategies(): List<StrategyExecutionDto>
}
