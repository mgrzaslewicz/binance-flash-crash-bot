package autocoin.binance.bot.strategy

import autocoin.binance.bot.exchange.PriceListener
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.exchange.currency.CurrencyPair


interface StrategyExecutorService : PriceListener {
    fun addStrategyExecutor(strategyParameters: StrategyParameters)
    fun currencyPairsCurrentlyNeeded(): List<CurrencyPair>
    fun addOrResumeStrategyExecutors(strategyParametersList: List<StrategyParameters>)
}
