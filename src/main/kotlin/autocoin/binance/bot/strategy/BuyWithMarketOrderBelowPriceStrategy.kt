package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.execution.StrategyExecution
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode


class BuyWithMarketOrderBelowPriceStrategy(
    /**
     * Sorted descending
     */
    private val pricesTriggeringBuyMarketOrder: List<BigDecimal>,
    private val counterCurrencyAmountLimitForBuying: BigDecimal,
) : Strategy {

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    private val numberOfBuyMarketOrdersToPlace = pricesTriggeringBuyMarketOrder.size
    private val counterCurrencyAmountPerOrder = counterCurrencyAmountLimitForBuying
        .divide(numberOfBuyMarketOrdersToPlace.toBigDecimal(), mathContext)

    private fun StrategyExecution.hasNoMaximumNumberOfOrdersYet() = orders.size < numberOfBuyMarketOrdersToPlace

    override fun getActions(price: BigDecimal, strategyExecution: StrategyExecution): List<StrategyAction> {
        return if (strategyExecution.hasNoMaximumNumberOfOrdersYet()) {
            val currentPricePoint = pricesTriggeringBuyMarketOrder[strategyExecution.orders.size]
            if (price < currentPricePoint) {
                listOf(PlaceBuyMarketOrderAction(counterCurrencyAmount = counterCurrencyAmountPerOrder, shouldBreakActionChainOnFail = true))
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    class Builder {
        companion object {
            val pricesTriggeringBuyMarketOrderParameter = "pricesTriggeringBuyMarketOrder"
            val counterCurrencyAmountLimitForBuyingParameter = "counterCurrencyAmountLimitForBuying"
        }

        private var pricesTriggeringBuyMarketOrder: MutableList<BigDecimal> = mutableListOf()
        private lateinit var counterCurrencyAmountLimitForBuying: BigDecimal

        fun withPricesTriggeringBuyMarketOrder(pricesTriggeringBuyMarketOrder: List<BigDecimal>): Builder {
            this.pricesTriggeringBuyMarketOrder = pricesTriggeringBuyMarketOrder.toMutableList()
            return this
        }

        fun withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying: BigDecimal): Builder {
            this.counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying
            return this
        }

        fun withStrategySpecificParameters(parameters: Map<String, String>): Builder {
            parameters.getValue(pricesTriggeringBuyMarketOrderParameter).let {
                pricesTriggeringBuyMarketOrder = it.split(",").map { price -> price.trim().toBigDecimal() }.toMutableList()
            }
            parameters.getValue(counterCurrencyAmountLimitForBuyingParameter).let {
                counterCurrencyAmountLimitForBuying = it.toBigDecimal()
            }
            return this
        }

        fun build(): BuyWithMarketOrderBelowPriceStrategy {
            return BuyWithMarketOrderBelowPriceStrategy(
                pricesTriggeringBuyMarketOrder = pricesTriggeringBuyMarketOrder.sortedByDescending { it },
                counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
            )
        }
    }

}
