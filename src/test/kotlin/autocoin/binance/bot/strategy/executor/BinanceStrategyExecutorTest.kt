package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.TestConfig.currencyPairWithPrice
import autocoin.binance.bot.exchange.TestOrderService
import autocoin.binance.bot.exchange.TestWalletService
import autocoin.binance.bot.strategy.Strategy
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.action.StrategyActionExecutor
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.TestStrategyExecutionMutableSet
import com.autocoin.exchangegateway.api.exchange.order.Order
import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BinanceStrategyExecutorTest {
    private companion object : KLogging()

    private lateinit var orderService: TestOrderService
    private lateinit var walletService: TestWalletService
    private lateinit var tested: BinanceStrategyExecutor
    private val walletCurrencyAmountAvailable = mapOf(
        TestConfig.currencyPair.base to BigDecimal("1100.78961234"),
    )

    @BeforeEach
    fun setup() {
        orderService = TestOrderService()
        walletService = TestWalletService(walletCurrencyAmountAvailable)
    }

    @Test
    fun shouldAdjustOrderAmountAndScale() {
        // given
        tested = BinanceStrategyExecutor(
            strategyExecution = TestConfig.samplePositionBuyLimitOrdersStrategyExecution(),
            orderServiceGateway = orderService,
            walletServiceGateway = walletService,
            strategyExecutions = TestStrategyExecutionMutableSet.get(),
            baseCurrencyAmountScale = 5,
            counterCurrencyPriceScale = 2,
            strategy = object : Strategy {
                override fun getActions(
                    price: BigDecimal,
                    strategyExecution: StrategyExecutionDto,
                ): List<StrategyAction> {
                    return listOf(
                        PlaceBuyLimitOrderAction(
                            price = price,
                            amount = 456.987654321.toBigDecimal(),
                        )
                    )
                }

            }
        )
        // when
        tested.onPriceUpdated(currencyPairWithPrice(16000.123456789.toBigDecimal()))
        // then
        assertThat(orderService.successfulActionHistory).hasSize(1)
        assertThat((orderService.successfulActionHistory[0] as Order).price).isEqualTo(16000.12.toBigDecimal())
        assertThat((orderService.successfulActionHistory[0] as Order).orderedAmount).isEqualTo(456.98765.toBigDecimal())
    }

    @Test
    fun shouldAdjustWithdrawScale() {
        // given
        tested = BinanceStrategyExecutor(
            strategyExecution = TestConfig.samplePositionBuyLimitOrdersStrategyExecution(),
            orderServiceGateway = orderService,
            walletServiceGateway = walletService,
            strategyExecutions = TestStrategyExecutionMutableSet.get(),
            baseCurrencyAmountScale = 5,
            counterCurrencyPriceScale = 2,
            strategy = object : Strategy {
                override fun getActions(
                    price: BigDecimal,
                    strategyExecution: StrategyExecutionDto,
                ): List<StrategyAction> = emptyList()

            }
        )
        // when
        tested.withdraw(currency = TestConfig.currencyPair.base, walletAddress = "test")
        // then
        assertThat(walletService.withdrawals).hasSize(1)
        assertThat(walletService.withdrawals[0].amount).isEqualTo(1100.78961.toBigDecimal())
    }

    @Test
    @Ignore("TDD: red")
    fun shouldSkipNextActionWhenPreviousFails() {
        // given
        var secondActionRun = false
        tested = BinanceStrategyExecutor(
            strategyExecution = TestConfig.samplePositionBuyLimitOrdersStrategyExecution(),
            orderServiceGateway = orderService,
            walletServiceGateway = walletService,
            strategyExecutions = TestStrategyExecutionMutableSet.get(),
            baseCurrencyAmountScale = 5,
            counterCurrencyPriceScale = 2,
            strategy = object : Strategy {
                override fun getActions(
                    price: BigDecimal,
                    strategyExecution: StrategyExecutionDto,
                ): List<StrategyAction> {
                    return listOf(
                        object : StrategyAction {
                            override fun apply(strategyExecutor: StrategyActionExecutor) = false
                        },
                        object : StrategyAction {
                            override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
                                secondActionRun = true
                                return true
                            }
                        }
                    )
                }
            }
        )
        // when
        tested.onPriceUpdated(currencyPairWithPrice(16000.123456789.toBigDecimal()))
        // then
        assertThat(secondActionRun).isFalse()
    }

}
