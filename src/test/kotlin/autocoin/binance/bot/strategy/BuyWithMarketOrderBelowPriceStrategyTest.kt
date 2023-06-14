package autocoin.binance.bot.strategy

import autocoin.binance.bot.TestConfig.samplePlaceBuyMarketOrdersBelowPriceStrategyParameters
import autocoin.binance.bot.strategy.PositionBuyOrdersForFlashCrashStrategyTest.TestStrategyExecutor
import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderAction
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto.Companion.toStrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.executor.StrategyExecutor
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class BuyWithMarketOrderBelowPriceStrategyTest {
    private val price1 = 100.toBigDecimal()
    private val price2 = 90.toBigDecimal()
    private val price3 = 80.toBigDecimal()
    private val strategyParameters: StrategyParametersDto = samplePlaceBuyMarketOrdersBelowPriceStrategyParameters(
        pricesTriggeringBuyMarketOrderParameter = listOf(price1, price2, price3),
        counterCurrencyAmountLimitForBuying = 150.toBigDecimal(),
    )
    private lateinit var strategyExecutor: StrategyExecutor
    private lateinit var tested: BuyWithMarketOrderBelowPriceStrategy

    @Test
    fun shouldBuyNothingWhenPriceAboveBuyPoint() {
        // given
        strategyExecutor = TestStrategyExecutor(strategyExecution = strategyParameters.toStrategyExecution())
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyParameters.strategySpecificParameters)
            .build()
        // when
        val actions = tested.getActions(
            price = 101.toBigDecimal(),
            strategyExecution = strategyExecutor.strategyExecution,
        )
        // then
        assertThat(actions).isEmpty()
    }

    @Test
    fun shouldBuyWhenPriceBelowFirstPricePoint() {
        // given
        strategyExecutor = TestStrategyExecutor(strategyExecution = strategyParameters.toStrategyExecution())
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyParameters.strategySpecificParameters)
            .build()
        // when
        val actions = tested.getActions(
            price = 99.99.toBigDecimal(),
            strategyExecution = strategyExecutor.strategyExecution,
        )
        // then
        assertThat(actions).hasSize(1)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(50.toBigDecimal())
    }

    @Test
    fun shouldNotBuyWhenPriceAboveNextPricePoint() {
        // given
        strategyExecutor = TestStrategyExecutor(
            strategyExecution = strategyParameters.toStrategyExecution().copy(
                orders = listOf(
                    StrategyOrder(
                        baseCurrencyCode = "BTC",
                        counterCurrencyCode = "USDT",
                        exchangeOrderId = "1",
                        amount = 30.toBigDecimal(),
                        price = 99.99.toBigDecimal(),
                        createTimeMillis = 1L,
                    ),
                )
            )
        )
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyParameters.strategySpecificParameters)
            .build()
        // when
        val actions = tested.getActions(
            price = 99.99.toBigDecimal(),
            strategyExecution = strategyExecutor.strategyExecution,
        )
        // then
        assertThat(actions).isEmpty()
    }

    @Test
    fun shouldBuyWhenPriceAboveNextPricePoint() {
        // given
        strategyExecutor = TestStrategyExecutor(
            strategyExecution = strategyParameters.toStrategyExecution().copy(
                orders = listOf(
                    StrategyOrder(
                        baseCurrencyCode = "BTC",
                        counterCurrencyCode = "USDT",
                        exchangeOrderId = "1",
                        amount = 30.toBigDecimal(),
                        price = 99.99.toBigDecimal(),
                        createTimeMillis = 1L,
                    ),
                )
            )
        )
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyParameters.strategySpecificParameters)
            .build()
        // when
        val actions = tested.getActions(
            price = 89.99.toBigDecimal(),
            strategyExecution = strategyExecutor.strategyExecution,
        )
        // then
        assertThat(actions).hasSize(1)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(50.toBigDecimal())
    }

    @Test
    fun shouldNotBuyWhenAllOrdersMade() {
        // given
        strategyExecutor = TestStrategyExecutor(
            strategyExecution = strategyParameters.toStrategyExecution().copy(
                orders = listOf(mock(), mock(), mock()),
            )
        )
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyParameters.strategySpecificParameters)
            .build()
        // when
        val actions = tested.getActions(
            price = 89.99.toBigDecimal(),
            strategyExecution = strategyExecutor.strategyExecution,
        )
        // then
        assertThat(actions).isEmpty()
    }

}
