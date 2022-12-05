package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.strategy.Strategy
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.FileBackedMutableSet
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.order.ExchangeCancelOrderParams
import automate.profit.autocoin.exchange.order.ExchangeOrder
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import automate.profit.autocoin.exchange.order.ExchangeOrderType
import mu.KLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.system.measureTimeMillis

class BinanceStrategyExecutor(
    strategyExecution: StrategyExecution,
    private val exchangeOrderService: ExchangeOrderService,
    private val strategyExecutions: FileBackedMutableSet<StrategyExecution>,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val javaExecutorService: ExecutorService,
    private val strategy: Strategy,
    private val baseCurrencyAmountScale: Int = 5,
    private val counterCurrencyPriceScale: Int = 2,
) : StrategyExecutor {
    private companion object : KLogging()

    private var currentStrategyExecution: StrategyExecution = strategyExecution.copy()
    private val preventFromStackingUpActionsLock: Lock = ReentrantLock()

    override val strategyExecution: StrategyExecution
        get() = currentStrategyExecution

    private fun previousActionsHaveFinished(): Boolean {
        return preventFromStackingUpActionsLock.tryLock()
    }

    private fun markActionsAsFinished() {
        preventFromStackingUpActionsLock.unlock()
    }

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        javaExecutorService.submit {
            val logTag = "user=${strategyExecution.userId}, currencyPair=${strategyExecution.currencyPair}, strategyType=${strategyExecution.strategyType}"
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


    override fun cancelOrder(order: StrategyOrder): Boolean {
        val success = exchangeOrderService.cancelOrder(
            exchangeName = SupportedExchange.BINANCE.exchangeName,
            exchangeKey = currentStrategyExecution.exchangeApiKey,
            ExchangeCancelOrderParams(
                exchangeName = SupportedExchange.BINANCE.exchangeName,
                orderId = order.exchangeOrderId,
                orderType = ExchangeOrderType.BID_BUY,
                currencyPair = currentStrategyExecution.currencyPair,
            )
        )
        if (success) {
            onBuyOrderCanceled(order)
        }
        return success
    }

    override fun placeBuyLimitOrder(buyPrice: BigDecimal, baseCurrencyAmount: BigDecimal): ExchangeOrder? {
        val buyPriceAdjusted = buyPrice.setScale(counterCurrencyPriceScale, RoundingMode.HALF_EVEN)
        val baseCurrencyAmountAdjusted = baseCurrencyAmount.setScale(baseCurrencyAmountScale, RoundingMode.DOWN)
        return try {
            val buyOrder = exchangeOrderService.placeLimitBuyOrder(
                exchangeName = SupportedExchange.BINANCE.exchangeName,
                exchangeKey = currentStrategyExecution.exchangeApiKey,
                baseCurrencyCode = currentStrategyExecution.baseCurrencyCode,
                counterCurrencyCode = currentStrategyExecution.counterCurrencyCode,
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

    override fun placeBuyMarketOrder(currentPrice: BigDecimal, counterCurrencyAmount: BigDecimal): ExchangeOrder? {
        val currentPriceAdjusted = currentPrice.setScale(counterCurrencyPriceScale, RoundingMode.HALF_EVEN)
        val counterCurrencyAmountAdjusted = counterCurrencyAmount.setScale(counterCurrencyPriceScale, RoundingMode.DOWN)
        return try {
            val buyOrder = exchangeOrderService.placeMarketBuyOrderWithCounterCurrencyAmount(
                exchangeName = SupportedExchange.BINANCE.exchangeName,
                exchangeKey = currentStrategyExecution.exchangeApiKey,
                baseCurrencyCode = currentStrategyExecution.baseCurrencyCode,
                counterCurrencyCode = currentStrategyExecution.counterCurrencyCode,
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

    private fun onBuyOrderCanceled(buyOrder: StrategyOrder) {
        currentStrategyExecution = currentStrategyExecution.copy(
            orders = currentStrategyExecution.orders - buyOrder
        )
        trySaveState()
    }

    private fun onBuyOrderPlaced(buyOrder: ExchangeOrder) {
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
