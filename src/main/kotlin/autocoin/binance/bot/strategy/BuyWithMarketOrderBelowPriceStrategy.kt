package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.action.WithdrawBaseCurrencyAction
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode


// TODO constructor parameters are the same parameters as provided by strategyExecution. Maybe we should just pass strategyExecution instead of all these parameters?
class BuyWithMarketOrderBelowPriceStrategy private constructor(
    private val pricesTriggeringBuyMarketOrderSortedDesc: List<BigDecimal>,
    private val counterCurrencyAmountLimitForBuying: BigDecimal,
    private val withdrawalAddress: String?,
) : Strategy {
    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    private val numberOfBuyMarketOrdersToPlace = pricesTriggeringBuyMarketOrderSortedDesc.size
    private val counterCurrencyAmountPerOrder = counterCurrencyAmountLimitForBuying
        .divide(numberOfBuyMarketOrdersToPlace.toBigDecimal(), mathContext)

    private var minimumReachedPriceTriggeringBuy: BigDecimal? = null
    private val plusInfinity = Integer.MAX_VALUE.toBigDecimal()

    private fun StrategyExecutionDto.hasNoMaximumNumberOfOrdersYet() = orders.size < numberOfBuyMarketOrdersToPlace

    override fun getActions(
        price: BigDecimal,
        strategyExecution: StrategyExecutionDto,
    ): List<StrategyAction> {
        return when {
            strategyExecution.hasNoMaximumNumberOfOrdersYet() -> {
                val currentPricePoint = pricesTriggeringBuyMarketOrderSortedDesc[strategyExecution.orders.size]
                when {
                    price >= (minimumReachedPriceTriggeringBuy ?: plusInfinity) -> {
                        listOfNotNull(
                            PlaceBuyMarketOrderAction(
                                currentPrice = price,
                                counterCurrencyAmount = counterCurrencyAmountLeft(strategyExecution.orders),
                                shouldBreakActionChainOnFail = true,
                            ),
                            if (withdrawalAddress != null) {
                                WithdrawBaseCurrencyAction(
                                    currency = strategyExecution.currencyPair.base,
                                    walletAddress = withdrawalAddress,
                                    shouldBreakActionChainOnFail = false,
                                )
                            } else {
                                null
                            }
                        )
                    }

                    price < currentPricePoint -> {
                        minimumReachedPriceTriggeringBuy = price.min(currentPricePoint)
                        listOfNotNull(
                            PlaceBuyMarketOrderAction(
                                currentPrice = price,
                                counterCurrencyAmount = counterCurrencyAmountPerOrder,
                                shouldBreakActionChainOnFail = true,
                            ),
                            if (withdrawalAddress != null) {
                                WithdrawBaseCurrencyAction(
                                    currency = strategyExecution.currencyPair.base,
                                    walletAddress = withdrawalAddress,
                                    shouldBreakActionChainOnFail = false,
                                )
                            } else {
                                null
                            }
                        )
                    }

                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }

    private fun counterCurrencyAmountLeft(orders: List<StrategyOrder>): BigDecimal {
        return counterCurrencyAmountLimitForBuying - orders.sumOf { it.amount }
    }

    class Builder {
        companion object {
            private val pricesTriggeringBuyMarketOrderParameter = "pricesTriggeringBuyMarketOrder"
            private val maxPriceForComingBackFromBottomBuyMarketOrderParameter =
                "maxPriceForComingBackFromBottomBuyMarketOrder"
            private val counterCurrencyAmountLimitForBuyingParameter = "counterCurrencyAmountLimitForBuying"
            private val withdrawalAddressParameter = "withdrawalAddress"
        }

        private var pricesTriggeringBuyMarketOrder: MutableList<BigDecimal> = mutableListOf()
        private lateinit var counterCurrencyAmountLimitForBuying: BigDecimal
        private lateinit var maxPriceForComingBackFromBottomBuyMarketOrder: BigDecimal
        private var withdrawalAddress: String? = null

        fun withPricesTriggeringBuyMarketOrder(pricesTriggeringBuyMarketOrder: List<BigDecimal>): Builder {
            this.pricesTriggeringBuyMarketOrder = pricesTriggeringBuyMarketOrder.toMutableList()
            return this
        }

        fun withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying: BigDecimal): Builder {
            this.counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying
            return this
        }

        fun withMaxPriceForComingBackFromBottomBuyMarketOrder(maxPriceForComingBackFromBottomBuyMarketOrder: BigDecimal): Builder {
            this.maxPriceForComingBackFromBottomBuyMarketOrder = maxPriceForComingBackFromBottomBuyMarketOrder
            return this
        }

        fun withWithdrawalAddress(withdrawalAddress: String): Builder {
            this.withdrawalAddress = withdrawalAddress
            return this
        }

        fun toStrategySpecificParameters(): Map<String, String> = mapOf(
            pricesTriggeringBuyMarketOrderParameter to pricesTriggeringBuyMarketOrder.joinToString(","),
            counterCurrencyAmountLimitForBuyingParameter to counterCurrencyAmountLimitForBuying.toPlainString(),
            maxPriceForComingBackFromBottomBuyMarketOrderParameter to maxPriceForComingBackFromBottomBuyMarketOrder.toPlainString(),
            withdrawalAddressParameter to withdrawalAddress,
        )
            .filterValues { it != null }
            .mapValues { it.value!! }

        fun withStrategySpecificParameters(parameters: Map<String, String>): Builder {
            parameters.getValue(pricesTriggeringBuyMarketOrderParameter).let {
                pricesTriggeringBuyMarketOrder = it.split(",")
                    .map { price -> price.trim().toBigDecimal() }
                    .toMutableList()
            }
            parameters.getValue(counterCurrencyAmountLimitForBuyingParameter).let {
                counterCurrencyAmountLimitForBuying = it.toBigDecimal()
            }
            parameters.getValue(maxPriceForComingBackFromBottomBuyMarketOrderParameter).let {
                maxPriceForComingBackFromBottomBuyMarketOrder = it.toBigDecimal()
            }
            withdrawalAddress = parameters[withdrawalAddressParameter]
            return this
        }

        fun build(): BuyWithMarketOrderBelowPriceStrategy {
            return BuyWithMarketOrderBelowPriceStrategy(
                pricesTriggeringBuyMarketOrderSortedDesc = pricesTriggeringBuyMarketOrder.sortedDescending(),
                counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
                withdrawalAddress = withdrawalAddress,
            )
        }
    }

}
