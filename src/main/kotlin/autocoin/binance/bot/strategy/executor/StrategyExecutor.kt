package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.PriceListener
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import com.autocoin.exchangegateway.spi.exchange.order.Order
import java.math.BigDecimal

interface StrategyExecutor : PriceListener {
    val strategyExecution: StrategyExecutionDto
    fun cancelOrder(order: StrategyOrder): Boolean
    fun placeBuyLimitOrder(
        buyPrice: BigDecimal,
        baseCurrencyAmount: BigDecimal,
    ): Order?

    fun placeBuyMarketOrder(
        currentPrice: BigDecimal,
        counterCurrencyAmount: BigDecimal,
    ): Order?
}
