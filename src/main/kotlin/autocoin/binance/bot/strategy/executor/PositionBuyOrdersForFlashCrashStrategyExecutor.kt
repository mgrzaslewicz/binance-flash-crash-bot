package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.order.ExchangeCancelOrderParams
import automate.profit.autocoin.exchange.order.ExchangeOrder
import automate.profit.autocoin.exchange.order.ExchangeOrderService
import automate.profit.autocoin.exchange.order.ExchangeOrderType
import automate.profit.autocoin.exchange.wallet.ExchangeWalletService
import mu.KLogging
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Clock

class PositionBuyOrdersForFlashCrashStrategyExecutor(
    override val strategyExecution: StrategyExecution,
    private val exchangeWalletService: ExchangeWalletService,
    private val exchangeOrderService: ExchangeOrderService,
    private val minPriceDownMultiplier: BigDecimal = 0.2.toBigDecimal(),
    private val lowestPriceUpdateRelativeThreshold: BigDecimal = 0.01.toBigDecimal(),
    private val strategyExecutionRepository: StrategyExecutionRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : StrategyExecutor {
    private companion object : KLogging()

    private val maxBigDecimal = Integer.MAX_VALUE.toBigDecimal()

    private val makePriceBitBiggerThanLowestLimit = BigDecimal(1.01)
    private var lowestPriceSoFar: BigDecimal = maxBigDecimal
    private var currentStrategyExecution: StrategyExecution = strategyExecution.copy()

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)

    private val baseCurrencyAmountScale = 6
    private val counterCurrencyPriceScale = 2
    private val counterCurrencyAmountPerOrder =
        currentStrategyExecution.counterCurrencyAmountLimitForBuying.divide(currentStrategyExecution.numberOfBuyLimitOrdersToKeep.toBigDecimal(), mathContext)

    override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        if (!shouldTryPlaceSomeOrders(currencyPairWithPrice.price)) {
            return
        }
        val lowPrice = currencyPairWithPrice.price
            .multiply(minPriceDownMultiplier, mathContext)
            .multiply(makePriceBitBiggerThanLowestLimit, mathContext)
            .setScale(counterCurrencyPriceScale, RoundingMode.HALF_EVEN)
        val currentLowestOrderPrice = currentStrategyExecution.orderWithMinPrice?.price ?: maxBigDecimal
        if (lowPrice < currentLowestOrderPrice) {
            val baseCurrencyAmount = counterCurrencyAmountPerOrder.divide(lowPrice, mathContext).setScale(baseCurrencyAmountScale, RoundingMode.HALF_EVEN)
            if (currentStrategyExecution.hasNoMaximumNumberOfOrdersYet) {
                fillUpToNBuyOrders(buyPrice = lowPrice, baseCurrencyAmount = baseCurrencyAmount)
            } else {
                cancelOrderWithHighestPrice()
                fillUpToNBuyOrders(buyPrice = lowPrice, baseCurrencyAmount = baseCurrencyAmount)
            }
        }
    }

    private fun cancelOrderWithHighestPrice() {
        if (cancelOrder(currentStrategyExecution.orderWithMaxPrice!!)) {
            onBuyOrderCanceled(currentStrategyExecution.orderWithMaxPrice!!)
        }
    }

    private fun cancelOrder(order: StrategyOrder): Boolean {
        return exchangeOrderService.cancelOrder(
            exchangeName = SupportedExchange.BINANCE.name, exchangeKey = currentStrategyExecution.exchangeApiKey, ExchangeCancelOrderParams(
                orderId = order.exchangeOrderId,
                orderType = ExchangeOrderType.BID_BUY,
                currencyPair = currentStrategyExecution.currencyPair,
            )
        )
    }

    private fun fillUpToNBuyOrders(buyPrice: BigDecimal, baseCurrencyAmount: BigDecimal) {
        repeat(currentStrategyExecution.numberOfBuyLimitOrdersToKeep - currentStrategyExecution.numberOfOrders) {
            tryToPlaceBuyLimitOrder(buyPrice = buyPrice, baseCurrencyAmount = baseCurrencyAmount)
        }
    }

    private fun tryToPlaceBuyLimitOrder(buyPrice: BigDecimal, baseCurrencyAmount: BigDecimal) {
        try {
            val buyOrder =
                exchangeOrderService.placeLimitBuyOrder(
                    exchangeName = SupportedExchange.BINANCE.exchangeName,
                    exchangeKey = currentStrategyExecution.exchangeApiKey,
                    baseCurrencyCode = currentStrategyExecution.baseCurrencyCode,
                    counterCurrencyCode = currentStrategyExecution.counterCurrencyCode,
                    buyPrice = buyPrice,
                    amount = baseCurrencyAmount,
                )
            onBuyOrderPlaced(buyOrder)
        } catch (e: Exception) {
            logger.error(e) { "Placing buy order failed" }
        }
    }

    private fun shouldTryPlaceSomeOrders(newPrice: BigDecimal): Boolean {
        return if (newPrice < lowestPriceSoFar) {
            val lowestPriceUpdateThreshold = lowestPriceSoFar - lowestPriceSoFar.multiply(lowestPriceUpdateRelativeThreshold, mathContext)
            if (newPrice < lowestPriceUpdateThreshold) {
                lowestPriceSoFar = newPrice
                true
            } else {
                currentStrategyExecution.hasNoMaximumNumberOfOrdersYet
            }
        } else {
            currentStrategyExecution.hasNoMaximumNumberOfOrdersYet
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
