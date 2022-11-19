package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.CancelOrderAction
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class PositionBuyOrdersForFlashCrashStrategy(
    private val minPriceDownMultiplier: BigDecimal = 0.2.toBigDecimal(),
    private val makePriceBitBiggerThanLowestLimit: BigDecimal = BigDecimal(1.01)
) : Strategy {

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    private val maxBigDecimal = Integer.MAX_VALUE.toBigDecimal()
    private var lowestPriceSoFar: BigDecimal = maxBigDecimal

    private fun fillUpToNBuyOrdersActions(buyPrice: BigDecimal, baseCurrencyAmount: BigDecimal, lackingOrders: Int): List<StrategyAction> {
        return (1..lackingOrders).map {
            PlaceBuyLimitOrderAction(
                price = buyPrice,
                amount = baseCurrencyAmount,
                shouldBreakActionChainOnFail = false,
            )
        }
    }

    override fun getActions(price: BigDecimal, strategyExecution: StrategyExecution): List<StrategyAction> {
        lowestPriceSoFar = lowestPriceSoFar.min(price)

        val lowBuyPrice = price
            .multiply(minPriceDownMultiplier, mathContext)
            .multiply(makePriceBitBiggerThanLowestLimit, mathContext)
        val counterCurrencyAmountPerOrder = strategyExecution.counterCurrencyAmountLimitForBuying
            .divide(strategyExecution.numberOfBuyLimitOrdersToKeep.toBigDecimal(), mathContext)
        val baseCurrencyAmount = counterCurrencyAmountPerOrder.divide(lowBuyPrice, mathContext)

        if (strategyExecution.hasNoMaximumNumberOfOrdersYet) {
            return fillUpToNBuyOrdersActions(
                lackingOrders = strategyExecution.numberOfBuyLimitOrdersToKeep - strategyExecution.numberOfOrders,
                buyPrice = lowBuyPrice,
                baseCurrencyAmount = baseCurrencyAmount
            )
        } else {
            if (lowBuyPrice < strategyExecution.orderWithMaxPrice!!.price) {
                return cancelOrderWithHighestPriceAndPlaceNewOne(
                    buyPrice = lowBuyPrice,
                    baseCurrencyAmount = baseCurrencyAmount,
                    strategyOrderWithMaxPrice = strategyExecution.orderWithMaxPrice,
                )
            }
        }
        return emptyList()
    }

    private fun cancelOrderWithHighestPriceAndPlaceNewOne(buyPrice: BigDecimal, baseCurrencyAmount: BigDecimal, strategyOrderWithMaxPrice: StrategyOrder): List<StrategyAction> {
        return listOf(
            CancelOrderAction(strategyOrder = strategyOrderWithMaxPrice, shouldBreakActionChainOnFail = true),
            PlaceBuyLimitOrderAction(
                price = buyPrice,
                amount = baseCurrencyAmount,
                shouldBreakActionChainOnFail = true,
            )
        )
    }

}
