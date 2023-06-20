package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.exchange.TestOrderService
import autocoin.binance.bot.exchange.TestWalletService
import autocoin.binance.bot.strategy.Strategy
import autocoin.binance.bot.strategy.action.PlaceBuyLimitOrderAction
import autocoin.binance.bot.strategy.action.StrategyAction
import autocoin.binance.bot.strategy.action.StrategyActionExecutor
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.execution.repository.TestStrategyExecutionMutableSet
import com.autocoin.exchangegateway.api.exchange.order.Order
import com.autocoin.exchangegateway.api.price.CurrencyPairWithPrice
import com.google.common.util.concurrent.MoreExecutors
import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

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

    private fun currencyPairWithPrice(price: BigDecimal) =
        CurrencyPairWithPrice(currencyPair = TestConfig.currencyPair, price = price)

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
            javaExecutorService = MoreExecutors.newDirectExecutorService(),
            strategy = object : Strategy {
                override fun getActions(
                    price: BigDecimal,
                    strategyExecution: StrategyExecutionDto,
                ): List<StrategyAction> {
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
            javaExecutorService = MoreExecutors.newDirectExecutorService(),
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
    fun shouldTakeNoActionsWhenPreviousActionsStillInProgress() {
        // given
        val blockForeverLock = ReentrantLock().apply { lock() }
        val handledPriceUpdateCounter = AtomicInteger(0)
        tested = BinanceStrategyExecutor(
            strategyExecution = TestConfig.samplePositionBuyLimitOrdersStrategyExecution(),
            orderServiceGateway = orderService,
            walletServiceGateway = walletService,
            strategyExecutions = TestStrategyExecutionMutableSet.get(),
            baseCurrencyAmountScale = 5,
            counterCurrencyPriceScale = 2,
            javaExecutorService = Executors.newFixedThreadPool(2), // 2 threads as 1st one is going to be block for scheduling. With only 1 thread, second price update would not be handled
            strategy = object : Strategy {
                override fun getActions(
                    price: BigDecimal,
                    strategyExecution: StrategyExecutionDto,
                ): List<StrategyAction> {
                    return listOf(
                        PlaceBuyLimitOrderAction(
                            price = price,
                            amount = 456.987654321.toBigDecimal(),
                            shouldBreakActionChainOnFail = false,
                        ),
                        object : StrategyAction {
                            override fun apply(strategyExecutor: StrategyActionExecutor): Boolean {
                                handledPriceUpdateCounter.incrementAndGet()
                                logger.info { "Blocking forever" }
                                blockForeverLock.lock()
                                return true
                            }

                            override val shouldBreakActionChainOnFail = true
                        },
                    )
                }

            }
        )
        // when
        val otherThreadExecutor = Executors.newSingleThreadExecutor()
        otherThreadExecutor.submit {
            tested.onPriceUpdated(currencyPairWithPrice(16000.123456789.toBigDecimal()))
            tested.onPriceUpdated(currencyPairWithPrice(16000.123456789.toBigDecimal()))
        }
        otherThreadExecutor.awaitTermination(1, TimeUnit.SECONDS)
        // then
        assertThat(orderService.successfulActionHistory).hasSize(1)
        assertThat(handledPriceUpdateCounter.get()).isEqualTo(1)
        assertThat((orderService.successfulActionHistory[0] as Order).price).isEqualTo(16000.12.toBigDecimal())
        assertThat((orderService.successfulActionHistory[0] as Order).orderedAmount).isEqualTo(456.98765.toBigDecimal())
    }

}
