package autocoin.binance.bot.strategy

import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.action.TryWithdrawBaseCurrencyAction
import autocoin.binance.bot.strategy.action.decorator.async
import autocoin.binance.bot.strategy.action.decorator.tryLock
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.parameters.WithStrategySpecificParameters
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


class BuyWithMarketOrderBelowPriceStrategy(private val jvmExecutorService: ExecutorService) : Strategy {
    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)

    private val preventFromParallelWithdrawalsLock: Lock = ReentrantLock()
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
                            ),
                            getWithdrawAction(strategySpecificParameters, strategyExecution)
                        )
                    }

                    price < currentPricePoint -> {
                        minimumReachedPriceTriggeringBuy = price.min(currentPricePoint)
                        listOfNotNull(
                            PlaceBuyMarketOrderAction(
                                currentPrice = price,
                                counterCurrencyAmount = strategySpecificParameters.counterCurrencyAmountPerOrder(),
                            ),
                            getWithdrawAction(strategySpecificParameters, strategyExecution)
                        )
                    }

                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }

    private fun getWithdrawAction(
        strategySpecificParameters: Parameters,
        strategyExecution: StrategyExecutionDto
    ) = if (strategySpecificParameters.withdrawalAddress != null) {
        TryWithdrawBaseCurrencyAction(
            currency = strategyExecution.currencyPair.base,
            walletAddress = strategySpecificParameters.withdrawalAddress,
        )
            .tryLock(preventFromParallelWithdrawalsLock)
            .async(jvmExecutorService)
    } else {
        null
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
