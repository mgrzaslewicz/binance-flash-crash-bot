package autocoin.binance.bot.strategy

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.strategy.action.CancelOrderAction
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderAction
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.executor.StrategyExecutor
import automate.profit.autocoin.exchange.order.ExchangeOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal

class PositionBuyOrdersForFlashCrashStrategyTest {
    private lateinit var tested: PositionBuyOrdersForFlashCrashStrategy
    private lateinit var strategyExecutor: StrategyExecutor

    private class TestStrategyExecutor(override val strategyExecution: StrategyExecution) : StrategyExecutor {

        override fun cancelOrder(order: StrategyOrder): Boolean {
            return true
        }

        override fun placeBuyLimitOrder(buyPrice: BigDecimal, baseCurrencyAmount: BigDecimal): ExchangeOrder? {
            return null
        }

        override fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice) {
        }

    }

    private val minPriceDownMultiplier = 0.2.toBigDecimal()

    @BeforeEach
    fun setup() {
        tested = PositionBuyOrdersForFlashCrashStrategy(minPriceDownMultiplier = minPriceDownMultiplier)
    }

    @Test
    fun shouldCreate4BuyLimitOrdersWhenNoneBefore() {
        // given
        strategyExecutor = TestStrategyExecutor(TestConfig.sampleStrategyExecution)
        // when
        val actions = tested.getActions(price = 16150.7.toBigDecimal(), strategyExecution = strategyExecutor.strategyExecution)
        // then
        assertThat(actions).hasSize(strategyExecutor.strategyExecution.numberOfBuyLimitOrdersToKeep)
        assertThat((actions[0] as PlaceBuyLimitOrderAction).price).isEqualTo(BigDecimal("3262.4414"))
    }

    @Test
    fun shouldCreate1BuyLimitOrdersWhen3Before() {
        // given
        strategyExecutor = TestStrategyExecutor(
            TestConfig.sampleStrategyExecution.copy(
                orders = listOf(
                    mock(),
                    mock(),
                    mock(),
                )
            )
        )
        // when
        val actions = tested.getActions(price = 16150.7.toBigDecimal(), strategyExecution = strategyExecutor.strategyExecution)
        // then
        assertThat(actions).hasSize(1)
        with(actions[0] as PlaceBuyLimitOrderAction) {
            assertThat(this.price).isEqualTo(BigDecimal("3262.4414"))
            assertThat(this.amount).isEqualTo(0.0015479205.toBigDecimal())
        }
    }

    @Test
    fun shouldRepositionOrderWithHighestPriceWhenLowPriceLowerThanHighestOrderPrice() {
        // given
        strategyExecutor = TestStrategyExecutor(
            TestConfig.sampleStrategyExecution.copy(
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
        )
        // when
        val actions = tested.getActions(price = 120.0.toBigDecimal(), strategyExecution = strategyExecutor.strategyExecution)
        // then
        assertThat(actions).hasSize(2)
        assertThat((actions[0] as CancelOrderAction).strategyOrder.id).isEqualTo(strategyExecutor.strategyExecution.orders[3].id)
        with((actions[1] as PlaceBuyLimitOrderAction)) {
            assertThat(this.price).isEqualTo(BigDecimal("24.240000"))
            assertThat(this.amount).isEqualTo(BigDecimal("0.20833333"))
        }
    }

}
