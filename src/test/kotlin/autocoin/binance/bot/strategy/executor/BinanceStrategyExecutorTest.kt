package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.exchange.TestOrderService
import autocoin.binance.bot.strategy.Strategy
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.execution.repository.TestStrategyExecutionRepository
import automate.profit.autocoin.exchange.order.ExchangeOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BinanceStrategyExecutorTest {
    private lateinit var orderService: TestOrderService
    private lateinit var tested: BinanceStrategyExecutor

    @BeforeEach
    fun setup() {
        orderService = TestOrderService()
    }

    private fun currencyPairWithPrice(price: BigDecimal) = CurrencyPairWithPrice(currencyPair = TestConfig.currencyPair, price = price)

    @Test
    fun shouldAdjustAmountAndScale() {
        // given
        tested = BinanceStrategyExecutor(
            strategyExecution = TestConfig.sampleStrategyParameters.toStrategyExecution(),
            exchangeOrderService = orderService,
            strategyExecutionRepository = TestStrategyExecutionRepository(),
            baseCurrencyAmountScale = 5,
            counterCurrencyPriceScale = 2,
            strategy = object : Strategy {
                override fun getActions(price: BigDecimal, strategyExecution: StrategyExecution): List<StrategyAction> {
                    return listOf(
                        PlaceBuyLimitOrderAction(
                            price = price,
                            amount = 456.987654321.toBigDecimal(),
                            shouldBreakActionChainOnFail = false,
                        )
                    )
                }

            }
        )
        // when
        tested.onPriceUpdated(currencyPairWithPrice(16000.123456789.toBigDecimal()))
        // then
        assertThat(orderService.successfulActionHistory).hasSize(1)
        assertThat((orderService.successfulActionHistory[0] as ExchangeOrder).price).isEqualTo(16000.12.toBigDecimal())
        assertThat((orderService.successfulActionHistory[0] as ExchangeOrder).orderedAmount).isEqualTo(456.98765.toBigDecimal())
    }

}
