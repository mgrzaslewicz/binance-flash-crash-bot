package autocoin.binance.bot.strategy

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.strategy.action.CancelOrderAction
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderAction
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal

class PositionBuyOrdersForFlashCrashStrategyTest {
    private lateinit var tested: PositionBuyOrdersForFlashCrashStrategy

    @BeforeEach
    fun setup() {
        tested = PositionBuyOrdersForFlashCrashStrategy()
    }

    @Test
    fun shouldCreate4BuyLimitOrdersWhenNoneBefore() {
        // given
        val numberOfBuyLimitOrdersToKeep = 4
        // when
        val actions =
            tested.getActions(
                price = 16150.7.toBigDecimal(),
                strategyExecution = TestConfig.samplePositionBuyLimitOrdersStrategyExecution(
                    numberOfBuyLimitOrdersToKeep = numberOfBuyLimitOrdersToKeep
                )
            )
        // then
        assertThat(actions).hasSize(numberOfBuyLimitOrdersToKeep)
        assertThat((actions[0] as PlaceBuyLimitOrderAction).price).isEqualTo(BigDecimal("3262.4414"))
    }

    @Test
    fun shouldCreate1BuyLimitOrdersWhen3Before() {
        // when
        val actions =
            tested.getActions(
                price = 16150.7.toBigDecimal(), strategyExecution =
                TestConfig.samplePositionBuyLimitOrdersStrategyExecution().copy(
                    orders = listOf(
                        mock(),
                        mock(),
                        mock(),
                    )
                )
            )
        // then
        assertThat(actions).hasSize(1)
        with(actions[0] as PlaceBuyLimitOrderAction) {
            assertThat(this.price).isEqualTo(BigDecimal("3262.4414"))
            assertThat(this.amount).isEqualTo(0.0076629729.toBigDecimal())
        }
    }

    @Test
    fun shouldRepositionOrderWithHighestPriceWhenLowPriceLowerThanHighestOrderPrice() {
        // given
        val strategyExecution = TestConfig.samplePositionBuyLimitOrdersStrategyExecution().copy(
            orders = listOf(
                StrategyOrder(
                    baseCurrencyCode = "BTC",
                    counterCurrencyCode = "USDT",
                    exchangeOrderId = "1",
                    amount = 30.toBigDecimal(),
                    price = 25.toBigDecimal(),
                    createTimeMillis = 1L,
                ),
                StrategyOrder(
                    baseCurrencyCode = "BTC",
                    counterCurrencyCode = "USDT",
                    exchangeOrderId = "1",
                    amount = 30.toBigDecimal(),
                    price = 25.toBigDecimal(),
                    createTimeMillis = 1L,
                ),
                StrategyOrder(
                    baseCurrencyCode = "BTC",
                    counterCurrencyCode = "USDT",
                    exchangeOrderId = "1",
                    amount = 30.toBigDecimal(),
                    price = 25.5.toBigDecimal(),
                    createTimeMillis = 1L,
                ),
                StrategyOrder(
                    baseCurrencyCode = "BTC",
                    counterCurrencyCode = "USDT",
                    exchangeOrderId = "1",
                    amount = 30.toBigDecimal(),
                    price = 26.toBigDecimal(),
                    createTimeMillis = 1L,
                ),
            )
        )
        // when
        val actions =
            tested.getActions(price = 120.0.toBigDecimal(), strategyExecution = strategyExecution)
        // then
        assertThat(actions).hasSize(2)
        assertThat((actions[0] as CancelOrderAction).strategyOrder.id).isEqualTo(strategyExecution.orders[3].id)
        with((actions[1] as PlaceBuyLimitOrderAction)) {
            assertThat(this.price).isEqualTo(BigDecimal("24.240000"))
            assertThat(this.amount).isEqualTo(BigDecimal("1.0313531"))
        }
    }

    @Test
    fun shouldNotRepositionOrderWithHighestPriceWhenPriceChangeBelowThreshold() {
        // given
        val strategyExecution = TestConfig.samplePositionBuyLimitOrdersStrategyExecution().copy(
            orders = listOf(
                StrategyOrder(
                    baseCurrencyCode = "BTC",
                    counterCurrencyCode = "USDT",
                    exchangeOrderId = "1",
                    amount = 30.toBigDecimal(),
                    price = 25.toBigDecimal(),
                    createTimeMillis = 1L,
                ),
                StrategyOrder(
                    baseCurrencyCode = "BTC",
                    counterCurrencyCode = "USDT",
                    exchangeOrderId = "1",
                    amount = 30.toBigDecimal(),
                    price = 25.toBigDecimal(),
                    createTimeMillis = 1L,
                ),
                StrategyOrder(
                    baseCurrencyCode = "BTC",
                    counterCurrencyCode = "USDT",
                    exchangeOrderId = "1",
                    amount = 30.toBigDecimal(),
                    price = 25.5.toBigDecimal(),
                    createTimeMillis = 1L,
                ),
                StrategyOrder(
                    baseCurrencyCode = "BTC",
                    counterCurrencyCode = "USDT",
                    exchangeOrderId = "1",
                    amount = 30.toBigDecimal(),
                    price = 26.toBigDecimal(),
                    createTimeMillis = 1L,
                ),
            )
        )
        // when
        val actions =
            tested.getActions(price = 128.0.toBigDecimal(), strategyExecution = strategyExecution)
        // then
        assertThat(actions).isEmpty()
    }

}
