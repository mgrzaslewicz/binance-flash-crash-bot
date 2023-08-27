package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.strategy.Strategy
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.FileBackedMutableSet
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import com.autocoin.exchangegateway.api.exchange.order.CancelOrderParams
import com.autocoin.exchangegateway.api.exchange.xchange.SupportedXchangeExchange.binance
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.order.Order
import com.autocoin.exchangegateway.spi.exchange.order.OrderSide
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import com.autocoin.exchangegateway.spi.exchange.price.CurrencyPairWithPrice
import com.autocoin.exchangegateway.spi.exchange.wallet.WithdrawResult
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import mu.KLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import kotlin.system.measureTimeMillis

class BinanceStrategyExecutor(
    strategyExecution: StrategyExecutionDto,
    private val orderServiceGateway: OrderServiceGateway<ApiKeyId>,
    private val walletServiceGateway: WalletServiceGateway<ApiKeyId>,
    private val strategyExecutions: FileBackedMutableSet<StrategyExecutionDto>,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val strategy: Strategy,
    private val baseCurrencyAmountScale: Int = 5,
    private val counterCurrencyPriceScale: Int = 2,
    private val exchange: Exchange = binance,
) : StrategyExecutor {
    private companion object : KLogging()

    private var currentStrategyExecution: StrategyExecutionDto = strategyExecution.copy()

    override val strategyExecution: StrategyExecutionDto
        get() = currentStrategyExecution

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        val logTag =
            "user=${strategyExecution.userId}, currencyPair=${strategyExecution.currencyPair}, strategyType=${strategyExecution.strategyType}"
        val actions = strategy.getActions(currencyPairWithPrice.price, currentStrategyExecution)
        var actionsExecutedSuccessfully = 0
        val millis = measureTimeMillis {
            actions.forEach { action ->
                if (action.apply(this)) {
                    actionsExecutedSuccessfully++
                } else {
                    logger.warn { "[$logTag] Skipping next action" }
                    return@measureTimeMillis
                }
            }
        }
        if (actions.isNotEmpty()) {
            logger.info { "[$logTag] Executed successfully $actionsExecutedSuccessfully of ${actions.size} actions in $millis ms." }
        }
    }

    /**
     * Underlying xchange implementation uses remote init lasting a few seconds.
     * Better prevent it rather than wait when there is need to create order immediately.
     */
    fun warmup() {
        logger.info { "Warming up strategy of user ${currentStrategyExecution.apiKeySupplier.id}" }
        orderServiceGateway.getOpenOrders(exchange = exchange, apiKey = currentStrategyExecution.apiKeySupplier)
    }

    override fun cancelOrder(order: StrategyOrder): Boolean {
        val success = orderServiceGateway.cancelOrder(
            exchange = exchange,
            apiKey = currentStrategyExecution.apiKeySupplier,
            cancelOrderParams = CancelOrderParams(
                exchange = exchange,
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
    ): Order {
        val buyPriceAdjusted = buyPrice.setScale(counterCurrencyPriceScale, RoundingMode.HALF_EVEN)
        val baseCurrencyAmountAdjusted = baseCurrencyAmount.setScale(baseCurrencyAmountScale, RoundingMode.DOWN)
        val buyOrder = orderServiceGateway.placeLimitBuyOrder(
            exchange = exchange,
            apiKey = currentStrategyExecution.apiKeySupplier,
            currencyPair = currentStrategyExecution.currencyPair,
            buyPrice = buyPriceAdjusted,
            amount = baseCurrencyAmountAdjusted,
        )
        onBuyOrderPlaced(buyOrder)
        return buyOrder
    }

    override fun placeBuyMarketOrder(
        currentPrice: BigDecimal,
        counterCurrencyAmount: BigDecimal,
    ): Order {
        val currentPriceAdjusted = currentPrice.setScale(counterCurrencyPriceScale, RoundingMode.HALF_EVEN)
        val counterCurrencyAmountAdjusted = counterCurrencyAmount.setScale(counterCurrencyPriceScale, RoundingMode.DOWN)
        val buyOrder = orderServiceGateway.placeMarketBuyOrderWithCounterCurrencyAmount(
            exchange = exchange,
            apiKey = currentStrategyExecution.apiKeySupplier,
            currencyPair = currentStrategyExecution.currencyPair,
            currentPrice = currentPriceAdjusted,
            counterCurrencyAmount = counterCurrencyAmountAdjusted,
        )
        onBuyOrderPlaced(buyOrder)
        return buyOrder
    }

    override fun withdraw(currency: String, walletAddress: String): WithdrawResult {
        val balance = walletServiceGateway.getCurrencyBalance(
            exchange = binance,
            apiKey = currentStrategyExecution.apiKeySupplier,
            currencyCode = currentStrategyExecution.currencyPair.base,
        )
        val amountAdjusted = balance.amountAvailable.setScale(baseCurrencyAmountScale, RoundingMode.DOWN)
        return walletServiceGateway.withdraw(
            exchange = binance,
            apiKey = currentStrategyExecution.apiKeySupplier,
            currencyCode = currentStrategyExecution.currencyPair.base,
            amount = amountAdjusted,
            address = walletAddress,
        )
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
