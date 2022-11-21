package autocoin.binance.bot.strategy

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderAction
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.executor.StrategyExecutor
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class BuyWithMarketOrderBelowPriceStrategyTest {
    private lateinit var strategyParameters: StrategyParameters
    private lateinit var strategyExecutor: StrategyExecutor
    private lateinit var tested: BuyWithMarketOrderBelowPriceStrategy

    @BeforeEach
    fun setup() {
        strategyParameters = TestConfig.samplePlaceBuyMarketOrdersBelowPriceStrategyParameters(
            pricesTriggeringBuyMarketOrderParameter = listOf(100.toBigDecimal(), 90.toBigDecimal(), 80.toBigDecimal()),
            counterCurrencyAmountLimitForBuying = 150.toBigDecimal(),
        )
    }

    @Test
    fun shouldBuyNothingWhenPriceAboveBuyPoint() {
        // given
        strategyExecutor = PositionBuyOrdersForFlashCrashStrategyTest.TestStrategyExecutor(strategyExecution = strategyParameters.toStrategyExecution())
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyParameters.strategySpecificParameters)
            .build()
        // when
        val actions = tested.getActions(price = 101.toBigDecimal(), strategyExecution = strategyExecutor.strategyExecution)
        // then
        assertThat(actions).isEmpty()
    }

    @Test
    fun shouldBuyWhenPriceBelowFirstPricePoint() {
        // given
        strategyExecutor = PositionBuyOrdersForFlashCrashStrategyTest.TestStrategyExecutor(strategyExecution = strategyParameters.toStrategyExecution())
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyParameters.strategySpecificParameters)
            .build()
        // when
        val actions = tested.getActions(price = 99.99.toBigDecimal(), strategyExecution = strategyExecutor.strategyExecution)
        // then
        assertThat(actions).hasSize(1)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(50.toBigDecimal())
    }

    @Test
    fun shouldNotBuyWhenPriceAboveNextPricePoint() {
        // given
        strategyExecutor = PositionBuyOrdersForFlashCrashStrategyTest.TestStrategyExecutor(
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
        val actions = tested.getActions(price = 99.99.toBigDecimal(), strategyExecution = strategyExecutor.strategyExecution)
        // then
        assertThat(actions).isEmpty()
    }

    @Test
    fun shouldBuyWhenPriceAboveNextPricePoint() {
        // given
        strategyExecutor = PositionBuyOrdersForFlashCrashStrategyTest.TestStrategyExecutor(
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
        val actions = tested.getActions(price = 89.99.toBigDecimal(), strategyExecution = strategyExecutor.strategyExecution)
        // then
        assertThat(actions).hasSize(1)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(50.toBigDecimal())
    }

    @Test
    fun shouldNotBuyWhenAllOrdersMade() {
        // given
        strategyExecutor = PositionBuyOrdersForFlashCrashStrategyTest.TestStrategyExecutor(
            strategyExecution = strategyParameters.toStrategyExecution().copy(
                orders = listOf(mock(), mock(), mock()),
            )
        )
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyParameters.strategySpecificParameters)
            .build()
        // when
        val actions = tested.getActions(price = 89.99.toBigDecimal(), strategyExecution = strategyExecutor.strategyExecution)
        // then
        assertThat(actions).isEmpty()
    }

}