package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.action.WithdrawBaseCurrencyAction
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.parameters.WithStrategySpecificParameters
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode


class BuyWithMarketOrderBelowPriceStrategy : Strategy {
    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)

    private var minimumReachedPriceTriggeringBuy: BigDecimal? = null
    private val plusInfinity = Integer.MAX_VALUE.toBigDecimal()
    private var parsedParameters: Parameters? = null
    private var strategyParametersHashCode: Int? = null

    override fun getActions(
        price: BigDecimal,
        strategyExecution: StrategyExecutionDto,
    ): List<StrategyAction> {
        val strategySpecificParameters = getStrategySpecificParameters(strategyExecution)
        val hasNoMaximumNumberOfOrdersYet =
            strategyExecution.orders.size < strategySpecificParameters.numberOfBuyMarketOrdersToPlace()
        return when {
            hasNoMaximumNumberOfOrdersYet -> {
                val currentPricePoint =
                    strategySpecificParameters.pricesTriggeringBuyMarketOrder[strategyExecution.orders.size]
                when {
                    price >= (minimumReachedPriceTriggeringBuy ?: plusInfinity) -> {
                        listOfNotNull(
                            PlaceBuyMarketOrderAction(
                                currentPrice = price,
                                counterCurrencyAmount = strategySpecificParameters.counterCurrencyAmountLeft(
                                    strategyExecution.orders
                                ),
                                shouldBreakActionChainOnFail = true,
                            ),
                            if (strategySpecificParameters.withdrawalAddress != null) {
                                WithdrawBaseCurrencyAction(
                                    currency = strategyExecution.currencyPair.base,
                                    walletAddress = strategySpecificParameters.withdrawalAddress,
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
                                counterCurrencyAmount = strategySpecificParameters.counterCurrencyAmountPerOrder(),
                                shouldBreakActionChainOnFail = true,
                            ),
                            if (strategySpecificParameters.withdrawalAddress != null) {
                                WithdrawBaseCurrencyAction(
                                    currency = strategyExecution.currencyPair.base,
                                    walletAddress = strategySpecificParameters.withdrawalAddress,
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

    private fun getStrategySpecificParameters(strategyExecution: StrategyExecutionDto): Parameters {
        if (strategyParametersHashCode != strategyExecution.parameters.hashCode()) {
            parsedParameters = ParametersBuilder().withStrategySpecificParameters(strategyExecution).toParameters()
            strategyParametersHashCode = strategyExecution.parameters.hashCode()
        }
        return parsedParameters!!
    }

    private fun Parameters.counterCurrencyAmountPerOrder() =
        counterCurrencyAmountLimitForBuying.divide(this.numberOfBuyMarketOrdersToPlace().toBigDecimal(), mathContext)

    private fun Parameters.counterCurrencyAmountLeft(orders: List<StrategyOrder>): BigDecimal {
        return counterCurrencyAmountLimitForBuying - orders.sumOf { it.amount }
    }

    companion object {
        private const val pricesTriggeringBuyMarketOrderParameter = "pricesTriggeringBuyMarketOrder"
        private const val maxPriceForComingBackFromBottomBuyMarketOrderParameter =
            "maxPriceForComingBackFromBottomBuyMarketOrder"
        private const val counterCurrencyAmountLimitForBuyingParameter = "counterCurrencyAmountLimitForBuying"
        private const val withdrawalAddressParameter = "withdrawalAddress"
    }

    data class Parameters(
        val pricesTriggeringBuyMarketOrder: List<BigDecimal>,
        val counterCurrencyAmountLimitForBuying: BigDecimal,
        val maxPriceForComingBackFromBottomBuyMarketOrder: BigDecimal,
        val withdrawalAddress: String?,
    ) {
        fun numberOfBuyMarketOrdersToPlace() = pricesTriggeringBuyMarketOrder.size
    }

    class ParametersBuilder {
        private var pricesTriggeringBuyMarketOrder: MutableList<BigDecimal> = mutableListOf()
        private lateinit var counterCurrencyAmountLimitForBuying: BigDecimal
        private lateinit var maxPriceForComingBackFromBottomBuyMarketOrder: BigDecimal
        private var withdrawalAddress: String? = null

        fun withPricesTriggeringBuyMarketOrder(pricesTriggeringBuyMarketOrder: List<BigDecimal>): ParametersBuilder {
            this.pricesTriggeringBuyMarketOrder = pricesTriggeringBuyMarketOrder.toMutableList()
            return this
        }

        fun withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying: BigDecimal): ParametersBuilder {
            this.counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying
            return this
        }

        fun withMaxPriceForComingBackFromBottomBuyMarketOrder(maxPriceForComingBackFromBottomBuyMarketOrder: BigDecimal): ParametersBuilder {
            this.maxPriceForComingBackFromBottomBuyMarketOrder = maxPriceForComingBackFromBottomBuyMarketOrder
            return this
        }

        fun withWithdrawalAddress(withdrawalAddress: String): ParametersBuilder {
            this.withdrawalAddress = withdrawalAddress
            return this
        }

        fun withStrategySpecificParameters(parameters: WithStrategySpecificParameters): ParametersBuilder {
            parameters.getParameter(pricesTriggeringBuyMarketOrderParameter).let {
                checkNotNull(it)
                pricesTriggeringBuyMarketOrder = it.split(",")
                    .map { price -> price.trim().toBigDecimal() }
                    .toMutableList()
            }
            parameters.getParameter(counterCurrencyAmountLimitForBuyingParameter).let {
                checkNotNull(it)
                counterCurrencyAmountLimitForBuying = it.toBigDecimal()
            }
            parameters.getParameter(maxPriceForComingBackFromBottomBuyMarketOrderParameter).let {
                checkNotNull(it)
                maxPriceForComingBackFromBottomBuyMarketOrder = it.toBigDecimal()
            }
            withdrawalAddress = parameters.getParameter(withdrawalAddressParameter)
            return this
        }

        fun toMap(): Map<String, String> = mapOf(
            pricesTriggeringBuyMarketOrderParameter to pricesTriggeringBuyMarketOrder.joinToString(","),
            counterCurrencyAmountLimitForBuyingParameter to counterCurrencyAmountLimitForBuying.toPlainString(),
            maxPriceForComingBackFromBottomBuyMarketOrderParameter to maxPriceForComingBackFromBottomBuyMarketOrder.toPlainString(),
            withdrawalAddressParameter to withdrawalAddress,
        )
            .filterValues { it != null }
            .mapValues { it.value!! }

        fun toParameters(): Parameters = Parameters(
            pricesTriggeringBuyMarketOrder = pricesTriggeringBuyMarketOrder.sortedDescending(),
            counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
            maxPriceForComingBackFromBottomBuyMarketOrder = maxPriceForComingBackFromBottomBuyMarketOrder,
            withdrawalAddress = withdrawalAddress,
        )

    }

}
