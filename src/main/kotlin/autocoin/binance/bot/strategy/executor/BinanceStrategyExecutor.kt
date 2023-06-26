package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.strategy.Strategy
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.FileBackedMutableSet
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import com.autocoin.exchangegateway.api.exchange.order.CancelOrderParams
import com.autocoin.exchangegateway.api.exchange.xchange.ExchangeNames.Companion.binance
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.order.Order
import com.autocoin.exchangegateway.spi.exchange.order.OrderSide
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import com.autocoin.exchangegateway.spi.exchange.price.CurrencyPairWithPrice
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import mu.KLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.system.measureTimeMillis

class BinanceStrategyExecutor(
    strategyExecution: StrategyExecutionDto,
    private val orderServiceGateway: OrderServiceGateway<ApiKeyId>,
    private val walletServiceGateway: WalletServiceGateway<ApiKeyId>,
    private val strategyExecutions: FileBackedMutableSet<StrategyExecutionDto>,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val javaExecutorService: ExecutorService,
    private val strategy: Strategy,
    private val baseCurrencyAmountScale: Int = 5,
    private val counterCurrencyPriceScale: Int = 2,
    private val exchangeName: ExchangeName = binance,
) : StrategyExecutor {
    private companion object : KLogging()

    private var currentStrategyExecution: StrategyExecutionDto = strategyExecution.copy()
    private val preventFromStackingUpActionsLock: Lock = ReentrantLock()

    override val strategyExecution: StrategyExecutionDto
        get() = currentStrategyExecution

    private fun previousActionsHaveFinished(): Boolean {
        return preventFromStackingUpActionsLock.tryLock()
    }

    private fun markActionsAsFinished() {
        preventFromStackingUpActionsLock.unlock()
    }

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        javaExecutorService.submit {
            val logTag =
                "user=${strategyExecution.userId}, currencyPair=${strategyExecution.currencyPair}, strategyType=${strategyExecution.strategyType}"
            if (previousActionsHaveFinished()) {
                try {
                    val actions = strategy.getActions(currencyPairWithPrice.price, currentStrategyExecution)
                    val millis = measureTimeMillis {
                        actions.forEach { action ->
                            if (!action.apply(this) && action.shouldBreakActionChainOnFail) {
                                logger.info { "[$logTag] Skipping next action" }
                                return@forEach
                            }
                        }
                    }
                    if (actions.isNotEmpty()) {
                        logger.info { "[$logTag] Actions executed in ${millis}ms. Number of actions=${actions.size}" }
                    }
                } finally {
                    markActionsAsFinished()
                }
            } else {
                logger.info { "[$logTag] Previous actions have not finished yet. Skipping this price update: $currencyPairWithPrice" }
            }
        }
    }

    /**
     * Underlying xchange implementation uses remote init lasting a few seconds.
     * Better prevent it rather than wait when there is need to create order immediately.
     */
    fun warmup() {
        logger.info { "Warming up strategy of user ${currentStrategyExecution.apiKeySupplier.id}" }
        orderServiceGateway.getOpenOrders(exchangeName = exchangeName, apiKey = currentStrategyExecution.apiKeySupplier)
    }

    override fun cancelOrder(order: StrategyOrder): Boolean {
        val success = orderServiceGateway.cancelOrder(
            exchangeName = exchangeName,
            apiKey = currentStrategyExecution.apiKeySupplier,
            cancelOrderParams = CancelOrderParams(
                exchangeName = exchangeName,
                orderId = order.exchangeOrderId,
                orderSide = OrderSide.BID_BUY,
                currencyPair = currentStrategyExecution.currencyPair,

                )
        )
        if (success) {
            onBuyOrderCanceled(order)
        }
        return success
    }

    override fun placeBuyLimitOrder(
        buyPrice: BigDecimal,
        baseCurrencyAmount: BigDecimal,
    ): Order? {
        val buyPriceAdjusted = buyPrice.setScale(counterCurrencyPriceScale, RoundingMode.HALF_EVEN)
        val baseCurrencyAmountAdjusted = baseCurrencyAmount.setScale(baseCurrencyAmountScale, RoundingMode.DOWN)
        return try {
            val buyOrder = orderServiceGateway.placeLimitBuyOrder(
                exchangeName = exchangeName,
                apiKey = currentStrategyExecution.apiKeySupplier,
                currencyPair = currentStrategyExecution.currencyPair,
                buyPrice = buyPriceAdjusted,
                amount = baseCurrencyAmountAdjusted,
            )
            onBuyOrderPlaced(buyOrder)
            buyOrder
        } catch (e: Exception) {
            logger.error(e) { "Placing buy order failed" }
            null
        }
    }

    override fun placeBuyMarketOrder(
        currentPrice: BigDecimal,
        counterCurrencyAmount: BigDecimal,
    ): Order? {
        val currentPriceAdjusted = currentPrice.setScale(counterCurrencyPriceScale, RoundingMode.HALF_EVEN)
        val counterCurrencyAmountAdjusted = counterCurrencyAmount.setScale(counterCurrencyPriceScale, RoundingMode.DOWN)
        return try {
            val buyOrder = orderServiceGateway.placeMarketBuyOrderWithCounterCurrencyAmount(
                exchangeName = exchangeName,
                apiKey = currentStrategyExecution.apiKeySupplier,
                currencyPair = currentStrategyExecution.currencyPair,
                currentPrice = currentPriceAdjusted,
                counterCurrencyAmount = counterCurrencyAmountAdjusted,
            )
            onBuyOrderPlaced(buyOrder)
            return buyOrder
        } catch (e: Exception) {
            logger.error(e) { "Placing buy market order failed" }
            null
        }
    }

    override fun withdraw(currency: String, walletAddress: String): Boolean {
        return try {
            val balance = walletServiceGateway.getCurrencyBalance(
                exchangeName = binance,
                apiKey = currentStrategyExecution.apiKeySupplier,
                currencyCode = currentStrategyExecution.currencyPair.base,
            )
            val amountAdjusted = balance.amountAvailable.setScale(baseCurrencyAmountScale, RoundingMode.DOWN)
            try {
                walletServiceGateway.withdraw(
                    exchangeName = binance,
                    apiKey = currentStrategyExecution.apiKeySupplier,
                    currencyCode = currentStrategyExecution.currencyPair.base,
                    amount = amountAdjusted,
                    address = walletAddress,
                )
                true
            } catch (e: Exception) {
                logger.error(e) { "Withdraw failed" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Getting currency balance as withdraw step failed" }
            false
        }
    }

    private fun onBuyOrderCanceled(buyOrder: StrategyOrder) {
        currentStrategyExecution = currentStrategyExecution.copy(
            orders = currentStrategyExecution.orders - buyOrder
        )
        trySaveState()
    }

    private fun onBuyOrderPlaced(buyOrder: Order) {
        currentStrategyExecution = currentStrategyExecution.copy(
            orders = currentStrategyExecution.orders + StrategyOrder(
                exchangeOrderId = buyOrder.exchangeOrderId,
                status = buyOrder.status,
                price = buyOrder.price,
                amount = buyOrder.orderedAmount,
                amountFilled = buyOrder.filledAmount ?: BigDecimal.ZERO,
                baseCurrencyCode = buyOrder.currencyPair.base,
                counterCurrencyCode = buyOrder.currencyPair.counter,
                createTimeMillis = clock.millis(),
            )
        )
        trySaveState()
    }

    private fun trySaveState() {
        try {
            strategyExecutions.remove(currentStrategyExecution)
            strategyExecutions.add(currentStrategyExecution)
            strategyExecutions.save()
        } catch (e: Exception) {
            logger.error(e) { "Could not save state" }
        }
    }

}
