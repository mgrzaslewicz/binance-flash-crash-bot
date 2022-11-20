package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.strategy.PositionBuyOrdersForFlashCrashStrategy
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import java.util.concurrent.ExecutorService

interface StrategyExecutorProvider {
    fun createStrategyExecutor(strategyParameters: StrategyParameters): StrategyExecutor
    fun createStrategyExecutor(strategyExecution: StrategyExecution): StrategyExecutor
}


enum class StrategyType {
    POSITION_BUY_ORDERS_FOR_FLASH_CRASH,
    BUY_WITH_MARKET_ORDER_BELOW_PRICE,
}

class BinanceStrategyExecutorProvider(
    private val exchangeOrderService: ExchangeOrderService,
    private val strategyExecutionRepository: StrategyExecutionRepository,
    private val javaExecutorService: ExecutorService,
) : StrategyExecutorProvider {
    private fun StrategyParameters.toStrategy() = when (this.strategyType) {
        StrategyType.POSITION_BUY_ORDERS_FOR_FLASH_CRASH -> PositionBuyOrdersForFlashCrashStrategy.Builder().withStrategySpecificParameters(this.strategySpecificParameters).build()
        StrategyType.BUY_WITH_MARKET_ORDER_BELOW_PRICE -> throw NotImplementedError()
    }

    override fun createStrategyExecutor(strategyParameters: StrategyParameters): StrategyExecutor {
        return BinanceStrategyExecutor(
            strategyExecution = strategyParameters.toStrategyExecution(),
            exchangeOrderService = exchangeOrderService,
            strategyExecutionRepository = strategyExecutionRepository,
            strategy = strategyParameters.toStrategy(),
            javaExecutorService = javaExecutorService,
        )
    }

    private fun StrategyExecution.toStrategy() = when (this.strategyType) {
        StrategyType.POSITION_BUY_ORDERS_FOR_FLASH_CRASH -> PositionBuyOrdersForFlashCrashStrategy.Builder().withStrategySpecificParameters(this.strategySpecificParameters).build()
        StrategyType.BUY_WITH_MARKET_ORDER_BELOW_PRICE -> throw NotImplementedError()
    }

    override fun createStrategyExecutor(strategyExecution: StrategyExecution): StrategyExecutor {
        return BinanceStrategyExecutor(
            strategyExecution = strategyExecution,
            exchangeOrderService = exchangeOrderService,
            strategyExecutionRepository = strategyExecutionRepository,
            strategy = strategyExecution.toStrategy(),
            javaExecutorService = javaExecutorService,
        )
    }
}
