package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.strategy.BuyWithMarketOrderBelowPriceStrategy
import autocoin.binance.bot.strategy.PositionBuyOrdersForFlashCrashStrategy
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto.Companion.toStrategyExecution
import autocoin.binance.bot.strategy.execution.repository.FileBackedMutableSet
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import autocoin.binance.bot.strategy.parameters.WithStrategySpecificParameters
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import java.util.concurrent.ExecutorService

interface StrategyExecutorProvider {
    fun createStrategyExecutor(strategyParameters: StrategyParametersDto): StrategyExecutor
    fun createStrategyExecutor(strategyExecution: StrategyExecutionDto): StrategyExecutor
}


enum class StrategyType {
    POSITION_BUY_ORDERS_FOR_FLASH_CRASH,
    BUY_WITH_MARKET_ORDER_BELOW_PRICE,
}

class BinanceStrategyExecutorProvider(
    private val orderServiceGateway: OrderServiceGateway<ApiKeyId>,
    private val walletServiceGateway: WalletServiceGateway<ApiKeyId>,
    private val strategyExecutions: FileBackedMutableSet<StrategyExecutionDto>,
    private val javaExecutorService: ExecutorService,
) : StrategyExecutorProvider {

    private fun WithStrategySpecificParameters.toStrategy(strategyType: StrategyType) = when (strategyType) {
        StrategyType.POSITION_BUY_ORDERS_FOR_FLASH_CRASH -> PositionBuyOrdersForFlashCrashStrategy
            .Builder()
            .withStrategySpecificParameters(this)
            .build()

        StrategyType.BUY_WITH_MARKET_ORDER_BELOW_PRICE -> BuyWithMarketOrderBelowPriceStrategy
            .Builder()
            .withStrategySpecificParameters(this)
            .build()
    }

    override fun createStrategyExecutor(strategyParameters: StrategyParametersDto): StrategyExecutor {
        return BinanceStrategyExecutor(
            strategyExecution = strategyParameters.toStrategyExecution(),
            orderServiceGateway = orderServiceGateway,
            walletServiceGateway = walletServiceGateway,
            strategyExecutions = strategyExecutions,
            strategy = strategyParameters.toStrategy(strategyParameters.strategyType),
            javaExecutorService = javaExecutorService,
        ).apply { warmup() }
    }

    override fun createStrategyExecutor(strategyExecution: StrategyExecutionDto): StrategyExecutor {
        return BinanceStrategyExecutor(
            strategyExecution = strategyExecution,
            orderServiceGateway = orderServiceGateway,
            walletServiceGateway = walletServiceGateway,
            strategyExecutions = strategyExecutions,
            strategy = strategyExecution.toStrategy(strategyExecution.strategyType),
            javaExecutorService = javaExecutorService,
        ).apply { warmup() }
    }
}
