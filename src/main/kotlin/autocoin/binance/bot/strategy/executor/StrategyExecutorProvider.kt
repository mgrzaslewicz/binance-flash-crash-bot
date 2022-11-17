package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import automate.profit.autocoin.exchange.wallet.ExchangeWalletService

interface StrategyExecutorProvider {
    fun createStrategyExecutor(strategyParameters: StrategyParameters): StrategyExecutor
    fun createStrategyExecutor(strategyExecution: StrategyExecution): StrategyExecutor
}

class DefaultStrategyExecutorProvider(
    private val exchangeWalletService: ExchangeWalletService,
    private val exchangeOrderService: ExchangeOrderService,
    private val strategyExecutionRepository: StrategyExecutionRepository,
) : StrategyExecutorProvider {
    override fun createStrategyExecutor(strategyParameters: StrategyParameters): StrategyExecutor {
        return PositionBuyOrdersForFlashCrashStrategyExecutor(
            strategyExecution = strategyParameters.toStrategyExecution(),
            exchangeWalletService = exchangeWalletService,
            exchangeOrderService = exchangeOrderService,
            strategyExecutionRepository = strategyExecutionRepository,
        )
    }

    override fun createStrategyExecutor(strategyExecution: StrategyExecution): StrategyExecutor {
        return PositionBuyOrdersForFlashCrashStrategyExecutor(
            strategyExecution = strategyExecution,
            exchangeWalletService = exchangeWalletService,
            exchangeOrderService = exchangeOrderService,
            strategyExecutionRepository = strategyExecutionRepository,
        )
    }
}
