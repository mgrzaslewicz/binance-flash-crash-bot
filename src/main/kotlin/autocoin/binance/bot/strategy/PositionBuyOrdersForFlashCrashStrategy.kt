package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.CancelOrderAction
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

// TODO constructor parameters are the same parameters as provided by strategyExecution. Maybe we should just pass strategyExecution instead of all these parameters?
class PositionBuyOrdersForFlashCrashStrategy private constructor(
    private val minPriceDownMultiplier: BigDecimal,
    private val makePriceBitBiggerThanLowestLimit: BigDecimal,
    private val orderRepositionRelativeDropThreshold: BigDecimal,
    private val numberOfBuyLimitOrdersToKeep: Int,
    private val counterCurrencyAmountLimitForBuying: BigDecimal,
) : Strategy {

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    private val maxBigDecimal = Integer.MAX_VALUE.toBigDecimal()
    private var lowestPriceSoFar: BigDecimal = maxBigDecimal

    private fun fillUpToNBuyOrdersActions(
        buyPrice: BigDecimal,
        baseCurrencyAmount: BigDecimal,
        lackingOrders: Int
    ): List<StrategyAction> {
        return (1..lackingOrders).map {
            PlaceBuyLimitOrderAction(
                price = buyPrice,
                amount = baseCurrencyAmount,
                shouldBreakActionChainOnFail = false,
            )
        }
    }

    private fun StrategyExecutionDto.hasNoMaximumNumberOfOrdersYet() = orders.size < numberOfBuyLimitOrdersToKeep

    override fun getActions(
        price: BigDecimal,
        strategyExecution: StrategyExecutionDto,
    ): List<StrategyAction> {
        lowestPriceSoFar = lowestPriceSoFar.min(price)

        val lowBuyPrice = price
            .multiply(minPriceDownMultiplier, mathContext)
            .multiply(makePriceBitBiggerThanLowestLimit, mathContext)
        val counterCurrencyAmountPerOrder = counterCurrencyAmountLimitForBuying
            .divide(numberOfBuyLimitOrdersToKeep.toBigDecimal(), mathContext)
        val baseCurrencyAmount = counterCurrencyAmountPerOrder.divide(lowBuyPrice, mathContext)

        if (strategyExecution.hasNoMaximumNumberOfOrdersYet()) {
            return fillUpToNBuyOrdersActions(
                lackingOrders = numberOfBuyLimitOrdersToKeep - strategyExecution.numberOfOrders,
                buyPrice = lowBuyPrice,
                baseCurrencyAmount = baseCurrencyAmount
            )
        } else {
            val repositionPriceThreshold =
                strategyExecution.orderWithMaxPrice!!.price - strategyExecution.orderWithMaxPrice.price.multiply(
                    orderRepositionRelativeDropThreshold,
                    mathContext
                )
            if (lowBuyPrice < repositionPriceThreshold) {
                return cancelOrderWithHighestPriceAndPlaceNewOne(
                    buyPrice = lowBuyPrice,
                    baseCurrencyAmount = baseCurrencyAmount,
                    strategyOrderWithMaxPrice = strategyExecution.orderWithMaxPrice,
                )
            }
        }
        return emptyList()
    }

    private fun cancelOrderWithHighestPriceAndPlaceNewOne(
        buyPrice: BigDecimal,
        baseCurrencyAmount: BigDecimal,
        strategyOrderWithMaxPrice: StrategyOrder
    ): List<StrategyAction> {
        return listOf(
            CancelOrderAction(strategyOrder = strategyOrderWithMaxPrice, shouldBreakActionChainOnFail = true),
            PlaceBuyLimitOrderAction(
                price = buyPrice,
                amount = baseCurrencyAmount,
                shouldBreakActionChainOnFail = true,
            )
        )
    }

    class Builder {
        companion object {
            private val numberOfBuyLimitOrdersToKeepParameter = "numberOfBuyLimitOrdersToKeep"
            private val counterCurrencyAmountLimitForBuyingParameter = "counterCurrencyAmountLimitForBuying"
        }

        private var minPriceDownMultiplier: BigDecimal = 0.2.toBigDecimal()
        private var makePriceBitBiggerThanLowestLimit: BigDecimal = BigDecimal(1.01)
        private var orderRepositionRelativeDropThreshold: BigDecimal = BigDecimal(0.01)
        private var numberOfBuyLimitOrdersToKeep: Int = 4
        private lateinit var counterCurrencyAmountLimitForBuying: BigDecimal

        fun withMinPriceDownMultiplier(minPriceDownMultiplier: BigDecimal): Builder {
            this.minPriceDownMultiplier = minPriceDownMultiplier
            return this
        }

        fun withMakePriceBitBiggerThanLowestLimit(makePriceBitBiggerThanLowestLimit: BigDecimal): Builder {
            this.makePriceBitBiggerThanLowestLimit = makePriceBitBiggerThanLowestLimit
            return this
        }

        fun withOrderRepositionRelativeDropThreshold(orderRepositionRelativeDropThreshold: BigDecimal): Builder {
            this.orderRepositionRelativeDropThreshold = orderRepositionRelativeDropThreshold
            return this
        }

        fun withNumberOfBuyLimitOrdersToKeep(numberOfBuyLimitOrdersToKeep: Int): Builder {
            this.numberOfBuyLimitOrdersToKeep = numberOfBuyLimitOrdersToKeep
            return this
        }

        fun withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying: BigDecimal): Builder {
            this.counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying
            return this
        }

        fun toStrategySpecificParameters(): Map<String, String> = mapOf(
            numberOfBuyLimitOrdersToKeepParameter to numberOfBuyLimitOrdersToKeep.toString(),
            counterCurrencyAmountLimitForBuyingParameter to counterCurrencyAmountLimitForBuying.toPlainString(),
        )

        fun withStrategySpecificParameters(parameters: Map<String, String>): Builder {
            parameters.getValue(numberOfBuyLimitOrdersToKeepParameter).let {
                numberOfBuyLimitOrdersToKeep = it.toInt()
            }
            parameters.getValue(counterCurrencyAmountLimitForBuyingParameter).let {
                counterCurrencyAmountLimitForBuying = it.toBigDecimal()
            }
            return this
        }

        fun build(): PositionBuyOrdersForFlashCrashStrategy {
            return PositionBuyOrdersForFlashCrashStrategy(
                minPriceDownMultiplier = minPriceDownMultiplier,
                makePriceBitBiggerThanLowestLimit = makePriceBitBiggerThanLowestLimit,
                orderRepositionRelativeDropThreshold = orderRepositionRelativeDropThreshold,
                numberOfBuyLimitOrdersToKeep = numberOfBuyLimitOrdersToKeep,
                counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
            )
        }
    }

}
