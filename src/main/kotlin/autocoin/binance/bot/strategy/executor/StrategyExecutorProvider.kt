package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.strategy.StrategyParameters
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import automate.profit.autocoin.exchange.wallet.ExchangeWalletService

interface StrategyExecutorProvider {
    fun createStrategyExecutor(strategyParameters: StrategyParameters): StrategyExecutor
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

}
