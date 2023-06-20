package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.CancelOrderAction
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.parameters.WithStrategySpecificParameters
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class PositionBuyOrdersForFlashCrashStrategy : Strategy {
    private val minPriceDownMultiplier: BigDecimal = 0.2.toBigDecimal()
    private val makePriceBitBiggerThanLowestLimit: BigDecimal = BigDecimal(1.01)
    private val orderRepositionRelativeDropThreshold: BigDecimal = BigDecimal(0.01)

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    private val maxBigDecimal = Integer.MAX_VALUE.toBigDecimal()
    private var lowestPriceSoFar: BigDecimal = maxBigDecimal
    private var parsedParameters: Parameters? = null
    private var strategyParametersHashCode: Int? = null

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


    override fun getActions(
        price: BigDecimal,
        strategyExecution: StrategyExecutionDto,
    ): List<StrategyAction> {
        val strategySpecificParameters = getStrategySpecificParameters(strategyExecution)
        val hasNoMaximumNumberOfOrdersYet =
            strategyExecution.orders.size < strategySpecificParameters.numberOfBuyLimitOrdersToKeep
        lowestPriceSoFar = lowestPriceSoFar.min(price)

        val lowBuyPrice = price
            .multiply(minPriceDownMultiplier, mathContext)
            .multiply(makePriceBitBiggerThanLowestLimit, mathContext)
        val counterCurrencyAmountPerOrder = strategySpecificParameters.counterCurrencyAmountLimitForBuying
            .divide(strategySpecificParameters.numberOfBuyLimitOrdersToKeep.toBigDecimal(), mathContext)
        val baseCurrencyAmount = counterCurrencyAmountPerOrder.divide(lowBuyPrice, mathContext)

        if (hasNoMaximumNumberOfOrdersYet) {
            return fillUpToNBuyOrdersActions(
                lackingOrders = strategySpecificParameters.numberOfBuyLimitOrdersToKeep - strategyExecution.numberOfOrders,
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

    private fun getStrategySpecificParameters(strategyExecution: StrategyExecutionDto): Parameters {
        if (strategyParametersHashCode != strategyExecution.parameters.hashCode()) {
            parsedParameters = ParametersBuilder().withStrategySpecificParameters(strategyExecution).toParameters()
            strategyParametersHashCode = strategyExecution.parameters.hashCode()
        }
        return parsedParameters!!
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

    companion object {
        private const val numberOfBuyLimitOrdersToKeepParameter = "numberOfBuyLimitOrdersToKeep"
        private const val counterCurrencyAmountLimitForBuyingParameter = "counterCurrencyAmountLimitForBuying"
    }

    data class Parameters(
        val numberOfBuyLimitOrdersToKeep: Int,
        val counterCurrencyAmountLimitForBuying: BigDecimal,
    )

    class ParametersBuilder {
        private var numberOfBuyLimitOrdersToKeep: Int = 4
        private lateinit var counterCurrencyAmountLimitForBuying: BigDecimal

        fun withNumberOfBuyLimitOrdersToKeep(numberOfBuyLimitOrdersToKeep: Int): ParametersBuilder {
            this.numberOfBuyLimitOrdersToKeep = numberOfBuyLimitOrdersToKeep
            return this
        }

        fun withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying: BigDecimal): ParametersBuilder {
            this.counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying
            return this
        }

        fun toSMap(): Map<String, String> = mapOf(
            numberOfBuyLimitOrdersToKeepParameter to numberOfBuyLimitOrdersToKeep.toString(),
            counterCurrencyAmountLimitForBuyingParameter to counterCurrencyAmountLimitForBuying.toPlainString(),
        )

        fun withStrategySpecificParameters(parameters: WithStrategySpecificParameters): ParametersBuilder {
            parameters.getParameter(numberOfBuyLimitOrdersToKeepParameter).let {
                checkNotNull(it)
                numberOfBuyLimitOrdersToKeep = it.toInt()
            }
            parameters.getParameter(counterCurrencyAmountLimitForBuyingParameter).let {
                checkNotNull(it)
                counterCurrencyAmountLimitForBuying = it.toBigDecimal()
            }
            return this
        }

        fun toParameters(): Parameters {
            return Parameters(
                numberOfBuyLimitOrdersToKeep = numberOfBuyLimitOrdersToKeep,
                counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
            )
        }
    }

}
