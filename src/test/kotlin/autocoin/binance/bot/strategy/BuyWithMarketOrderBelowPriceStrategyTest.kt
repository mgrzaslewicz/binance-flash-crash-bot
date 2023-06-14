package autocoin.binance.bot.strategy

import autocoin.binance.bot.TestConfig.samplePlaceBuyMarketOrdersBelowPriceStrategyParameters
import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderAction
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto.Companion.toStrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class BuyWithMarketOrderBelowPriceStrategyTest {
    private val price1 = 100.toBigDecimal()
    private val price2 = 90.toBigDecimal()
    private val price3 = 80.toBigDecimal()
    private val smallDelta = 0.01.toBigDecimal()
    private val allPrices = listOf(price1, price2, price3)
    private val counterCurrencyAmountLimitForBuying = 150.toBigDecimal()
    private val strategyParameters: StrategyParametersDto = samplePlaceBuyMarketOrdersBelowPriceStrategyParameters(
        pricesTriggeringBuyMarketOrderParameter = allPrices,
        counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying
    )
    private val strategyExecution = strategyParameters.toStrategyExecution()
    private lateinit var tested: BuyWithMarketOrderBelowPriceStrategy

    @BeforeEach
    fun setup() {
        tested = BuyWithMarketOrderBelowPriceStrategy.Builder()
            .withStrategySpecificParameters(strategyExecution.strategySpecificParameters)
            .build()
    }

    @Test
    fun shouldBuyNothingWhenPriceAboveBuyPoint() {
        // when
        val actions = tested.getActions(
            price = price1.plus(smallDelta),
            strategyExecution = strategyExecution,
        )
        // then
        assertThat(actions).isEmpty()
    }

    @Test
    fun shouldBuyWhenPriceBelowFirstPricePoint() {
        // when
        val actions = tested.getActions(
            price = price1.minus(smallDelta),
            strategyExecution = strategyExecution,
        )
        // then
        assertThat(actions).hasSize(1)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(50.toBigDecimal())
    }

    @Test
    fun shouldNotBuyWhenPriceAboveNextPricePoint() {
        // when
        val actions = tested.getActions(
            price = price1.minus(smallDelta),
            strategyExecution = strategyExecution.copy(
                orders = listOf(
                    StrategyOrder(
                        baseCurrencyCode = "BTC",
                        counterCurrencyCode = "USDT",
                        exchangeOrderId = "1",
                        amount = 30.toBigDecimal(),
                        price = price1.minus(0.01.toBigDecimal()),
                        createTimeMillis = 1L,
                    ),
                )
            ),
        )
        // then
        assertThat(actions).isEmpty()
    }

    @Test
    fun shouldBuyWhenPriceAboveNextPricePoint() {
        // when
        val actions = tested.getActions(
            price = price2.minus(smallDelta),
            strategyExecution = strategyExecution.copy(
                orders = listOf(
                    StrategyOrder(
                        baseCurrencyCode = "BTC",
                        counterCurrencyCode = "USDT",
                        exchangeOrderId = "1",
                        amount = 30.toBigDecimal(),
                        price = price1.minus(smallDelta),
                        createTimeMillis = 1L,
                    ),
                )
            ),
        )
        // then
        assertThat(actions).hasSize(1)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(50.toBigDecimal())
    }

    @Test
    fun shouldNotBuyWhenAllOrdersMade() {
        // given
        val ordersForEveryPricePoint = allPrices.map { mock<StrategyOrder>() }
        // when
        val actions = tested.getActions(
            price = price2.minus(smallDelta),
            strategyExecution = strategyExecution.copy(orders = ordersForEveryPricePoint),
        )
        // then
        assertThat(actions).isEmpty()
    }

    @Test
    fun shouldBuyWithAllCurrencyWhenPriceGoesUpFromTheBottom() {
        // when
        tested.getActions(price = price2, strategyExecution = strategyExecution)
        val actions = tested.getActions(price = price2.plus(smallDelta), strategyExecution = strategyExecution)
        // then
        assertThat(actions).hasSize(1)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(
            counterCurrencyAmountLimitForBuying
        )
    }

    @Test
    fun shouldBuyWithAllLeftCurrencyWhenPriceGoesUpFromTheBottom() {
        // when
        tested.getActions(price = price2, strategyExecution = strategyExecution)
        val actions = tested.getActions(
            price = price1,
            strategyExecution = strategyExecution.copy(
                orders = listOf(
                    mock<StrategyOrder>().apply {
                        whenever(this.amount).thenReturn(counterCurrencyAmountLimitForBuying.minus(BigDecimal.TEN))
                    }
                )
            ),
        )
        // then
        assertThat(actions).hasSize(1)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(BigDecimal.TEN)
    }

}
