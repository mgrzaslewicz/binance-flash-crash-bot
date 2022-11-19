package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.strategy.PositionBuyOrdersForFlashCrashStrategy
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import automate.profit.autocoin.exchange.wallet.ExchangeWalletService
import java.util.concurrent.ExecutorService

interface StrategyExecutorProvider {
    fun createStrategyExecutor(strategyParameters: StrategyParameters): StrategyExecutor
    fun createStrategyExecutor(strategyExecution: StrategyExecution): StrategyExecutor
}

class BinanceStrategyExecutorProvider(
    private val exchangeWalletService: ExchangeWalletService,
    private val exchangeOrderService: ExchangeOrderService,
    private val strategyExecutionRepository: StrategyExecutionRepository,
    private val javaExecutorService: ExecutorService,
) : StrategyExecutorProvider {
    override fun createStrategyExecutor(strategyParameters: StrategyParameters): StrategyExecutor {
        return BinanceStrategyExecutor(
            strategyExecution = strategyParameters.toStrategyExecution(),
            exchangeOrderService = exchangeOrderService,
            strategyExecutionRepository = strategyExecutionRepository,
            strategy = PositionBuyOrdersForFlashCrashStrategy(),
            javaExecutorService = javaExecutorService,
        )
    }

    override fun createStrategyExecutor(strategyExecution: StrategyExecution): StrategyExecutor {
        return BinanceStrategyExecutor(
            strategyExecution = strategyExecution,
            exchangeOrderService = exchangeOrderService,
            strategyExecutionRepository = strategyExecutionRepository,
            strategy = PositionBuyOrdersForFlashCrashStrategy(),
            javaExecutorService = javaExecutorService,
        )
    }
}
