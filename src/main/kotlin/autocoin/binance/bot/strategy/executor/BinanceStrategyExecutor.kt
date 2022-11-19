package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.strategy.Strategy
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
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

class BinanceStrategyExecutor(
    strategyExecution: StrategyExecution,
    private val exchangeOrderService: ExchangeOrderService,
    private val strategyExecutionRepository: StrategyExecutionRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val strategy: Strategy,
    private val baseCurrencyAmountScale: Int = 5,
    private val counterCurrencyPriceScale: Int = 2,
) : StrategyExecutor {
    private companion object : KLogging()

    private var currentStrategyExecution: StrategyExecution = strategyExecution.copy()

    override val strategyExecution: StrategyExecution
        get() = currentStrategyExecution


    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        strategy.getActions(currencyPairWithPrice.price, currentStrategyExecution).forEach { action ->
            if (!action.apply(this) && action.shouldBreakActionChainOnFail) {
                return
            }
        }
    }


    override fun cancelOrder(order: StrategyOrder): Boolean {
        val success = exchangeOrderService.cancelOrder(
            exchangeName = SupportedExchange.BINANCE.exchangeName, exchangeKey = currentStrategyExecution.exchangeApiKey, ExchangeCancelOrderParams(
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
            val buyOrder =
                exchangeOrderService.placeLimitBuyOrder(
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

    private fun onBuyOrderCanceled(buyOrder: StrategyOrder) {
        currentStrategyExecution = currentStrategyExecution.copy(
            orders = currentStrategyExecution.orders - buyOrder
        )
        trySaveState()
    }

    private fun onBuyOrderPlaced(buyOrder: ExchangeOrder) {
        currentStrategyExecution = currentStrategyExecution.copy(
            orders = currentStrategyExecution.orders + StrategyOrder(
                exchangeOrderId = buyOrder.orderId,
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
            strategyExecutionRepository.save(currentStrategyExecution)
        } catch (e: Exception) {
            logger.error(e) { "Could not save state" }
        }
    }

}
