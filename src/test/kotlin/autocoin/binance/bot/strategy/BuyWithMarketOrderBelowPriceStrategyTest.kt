package autocoin.binance.bot.strategy

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.exchange.apikey.ApiKeyDto
import autocoin.binance.bot.strategy.action.PlaceBuyMarketOrderAction
import autocoin.binance.bot.strategy.action.WithdrawActionExecutor
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto.Companion.toStrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.executor.StrategyType
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import com.google.common.util.concurrent.MoreExecutors
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
    private val maxPriceForComingBackFromBottomBuyMarketOrder = 10_000.toBigDecimal()
    private val smallDelta = 0.01.toBigDecimal()
    private val pricesTriggeringBuyMarketOrder = listOf(price1, price2, price3)
    private val counterCurrencyAmountLimitForBuying = 150.toBigDecimal()
    private val strategyParameters: StrategyParametersDto = StrategyParametersDto(
        baseCurrencyCode = TestConfig.currencyPair.base,
        counterCurrencyCode = TestConfig.currencyPair.counter,
        userId = "user-1",
        strategyType = StrategyType.BUY_WITH_MARKET_ORDER_BELOW_PRICE,
        apiKey = ApiKeyDto(
            publicKey = "key-1",
            secretKey = "secret-1",
        ),
        strategySpecificParameters = BuyWithMarketOrderBelowPriceStrategy.ParametersBuilder()
            .withPricesTriggeringBuyMarketOrder(pricesTriggeringBuyMarketOrder)
            .withMaxPriceForComingBackFromBottomBuyMarketOrder(maxPriceForComingBackFromBottomBuyMarketOrder)
            .withCounterCurrencyAmountLimitForBuying(counterCurrencyAmountLimitForBuying)
            .toMap()
    )
    private val strategyExecution = strategyParameters.toStrategyExecution()
    private lateinit var tested: BuyWithMarketOrderBelowPriceStrategy

    @BeforeEach
    fun setup() {
        tested = BuyWithMarketOrderBelowPriceStrategy(executorService = MoreExecutors.newDirectExecutorService())
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
    fun shouldBuyAndWithdrawWhenPriceBelowFirstPricePoint() {
        // given
        val strategyExecution = strategyExecution.copy(
            parameters = strategyParameters.copy(
                strategySpecificParameters = BuyWithMarketOrderBelowPriceStrategy.ParametersBuilder()
                    .withStrategySpecificParameters(strategyExecution)
                    .withWithdrawalAddress("withdrawal-address-1")
                    .toMap()
            ),
        )
        // when
        val actions = tested.getActions(
            price = price1.minus(smallDelta),
            strategyExecution = strategyExecution
        )
        // then
        assertThat(actions).hasSize(2)
        assertThat((actions[0] as PlaceBuyMarketOrderAction).counterCurrencyAmount).isEqualTo(50.toBigDecimal())
        var isCurrencyWithdrawn = false
        actions[1].apply(object : WithdrawActionExecutor {
            override fun withdraw(currency: String, walletAddress: String): Boolean {
                isCurrencyWithdrawn = true
                return true
            }
        })
        assertThat(isCurrencyWithdrawn).isTrue()
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
        val ordersForEveryPricePoint = pricesTriggeringBuyMarketOrder.map { mock<StrategyOrder>() }
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
    fun shouldNotBuyWhenPriceGoesUpFromTheBottomAndPriceIsTooHigh() {
        // when
        tested.getActions(price = price1.minus(smallDelta), strategyExecution = strategyExecution)
        val actions = tested.getActions(
            price = maxPriceForComingBackFromBottomBuyMarketOrder.plus(smallDelta),
            strategyExecution = strategyExecution
        )
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
