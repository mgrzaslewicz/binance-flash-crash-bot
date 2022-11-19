package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.PriceListener
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import automate.profit.autocoin.exchange.order.ExchangeOrder
import java.math.BigDecimal

interface StrategyExecutor : PriceListener {
    val strategyExecution: StrategyExecution
    fun cancelOrder(order: StrategyOrder): Boolean
    fun placeBuyLimitOrder(buyPrice: BigDecimal, baseCurrencyAmount: BigDecimal): ExchangeOrder?
}
