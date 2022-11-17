package autocoin.binance.bot.strategy.executor

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.exchange.TestOrderService
import autocoin.binance.bot.exchange.TestWalletService
import autocoin.binance.bot.strategy.execution.repository.TestStrategyExecutionRepository
import automate.profit.autocoin.exchange.order.ExchangeCancelOrderParams
import automate.profit.autocoin.exchange.order.ExchangeOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PositionBuyOrdersForFlashCrashStrategyExecutorTest {
    private lateinit var walletService: TestWalletService
    private lateinit var orderService: TestOrderService
    private lateinit var tested: PositionBuyOrdersForFlashCrashStrategyExecutor

    private val lowestPriceUpdateRelativeThreshold = 0.01.toBigDecimal()

    @BeforeEach
    fun setup() {
        walletService = TestWalletService()
        orderService = TestOrderService()
        tested = PositionBuyOrdersForFlashCrashStrategyExecutor(
            strategyExecution = TestConfig.sampleStrategyParameters.toStrategyExecution(),
            exchangeWalletService = walletService,
            exchangeOrderService = orderService,
            strategyExecutionRepository = TestStrategyExecutionRepository(),
            lowestPriceUpdateRelativeThreshold = lowestPriceUpdateRelativeThreshold,
        )
    }

    private fun currencyPairWithPrice(price: BigDecimal) = CurrencyPairWithPrice(currencyPair = TestConfig.currencyPair, price = price)

    @Test
    fun shouldCreate4BuyLimitOrdersWhenNoneBefore() {
        // when
        tested.onPriceUpdated(currencyPairWithPrice(16150.7.toBigDecimal()))
        // then
        val orderPrice = BigDecimal("3262.44")
        assertThat(orderService.successfulActionHistory).hasSize(4)
        assertThat((orderService.successfulActionHistory[0] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[1] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[2] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[3] as ExchangeOrder).price).isEqualTo(orderPrice)
    }

    @Test
    fun shouldRetryCreating4BuyLimitOrdersWhenNoneBefore() {
        // given
        orderService.placeLimitBuyOrderInvocationFailureIndexes = listOf(0, 1, 2, 3)
        // when
        tested.onPriceUpdated(currencyPairWithPrice(16000.toBigDecimal()))
        tested.onPriceUpdated(currencyPairWithPrice(16000.toBigDecimal()))
        // then
        val orderPrice = BigDecimal("3232.00")
        assertThat(orderService.successfulActionHistory).hasSize(4)
        assertThat((orderService.successfulActionHistory[0] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[1] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[2] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[3] as ExchangeOrder).price).isEqualTo(orderPrice)
    }

    @Test
    fun shouldNotCreateNewBuyLimitOrdersWhenDidNotDropBelowThreshold() {
        // when
        tested.onPriceUpdated(currencyPairWithPrice(price = 16000.toBigDecimal()))
        val nextPriceNotBelowThreshold = 16000 - 160
        tested.onPriceUpdated(currencyPairWithPrice(price = nextPriceNotBelowThreshold.toBigDecimal()))
        // then
        val orderPrice = BigDecimal("3232.00")
        assertThat(orderService.successfulActionHistory).hasSize(4)
        assertThat((orderService.successfulActionHistory[0] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[1] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[2] as ExchangeOrder).price).isEqualTo(orderPrice)
        assertThat((orderService.successfulActionHistory[3] as ExchangeOrder).price).isEqualTo(orderPrice)
    }
    @Test
    fun shouldCreateBuyLimitOrderWithSecondPriceWhenDropBelowThreshold() {
        // when
        tested.onPriceUpdated(currencyPairWithPrice(price = 16000.toBigDecimal()))
        val nextPriceBelowThreshold = 16000 - 161
        tested.onPriceUpdated(currencyPairWithPrice(price = nextPriceBelowThreshold.toBigDecimal()))
        // then
        val numbersOfInitialOrdersCreated = 4
        val numberOfOrdersCanceled = 1
        val numberOfOrdersCreated = 1
        assertThat(orderService.successfulActionHistory).hasSize(numbersOfInitialOrdersCreated + numberOfOrdersCanceled + numberOfOrdersCreated)
        assertThat((orderService.successfulActionHistory[4] as ExchangeCancelOrderParams).orderId)
            .isEqualTo((orderService.successfulActionHistory[0] as ExchangeOrder).orderId)
        assertThat((orderService.successfulActionHistory[5] as ExchangeOrder).price).isEqualTo(BigDecimal("3199.48"))
    }
}
