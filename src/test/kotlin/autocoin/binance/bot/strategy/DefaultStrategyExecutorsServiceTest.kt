package autocoin.binance.bot.strategy

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.exchange.TestOrderService
import autocoin.binance.bot.exchange.TestWalletService
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionRepository
import autocoin.binance.bot.strategy.execution.repository.TestStrategyExecutionRepository
import autocoin.binance.bot.strategy.executor.*
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.exchange.currency.CurrencyPair
import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class DefaultStrategyExecutorsServiceTest {
    companion object : KLogging()


    @Mock
    private lateinit var strategyExecutionRepository: StrategyExecutionRepository

    private val currencyPair = CurrencyPair.of("A", "B")

    val strategy1Parameters = TestConfig.sampleStrategyParameters
    val strategy2Parameters = strategy1Parameters.copy(
        userId = "user-2"
    )

    @Test
    fun shouldNotAllowToAddSecondStrategyWIthTHeSameUserAndCurrencyPair() {
        // given
        val tested = ExchangeStrategyExecutorService(
            strategyExecutionRepository = strategyExecutionRepository,
            strategyExecutorProvider = BinanceStrategyExecutorProvider(
                exchangeWalletService = TestWalletService(),
                exchangeOrderService = TestOrderService(),
                strategyExecutionRepository = TestStrategyExecutionRepository(),
            )
        )
        // when
        tested.addStrategyExecutor(strategy1Parameters)
        assertThrows<Exception> {
            tested.addStrategyExecutor(strategy1Parameters)
        }
    }

    @Test
    fun shouldUpdatePrices() {
        // given
        val binanceStrategyExecutorProvider = BinanceStrategyExecutorProvider(
            exchangeWalletService = TestWalletService(),
            exchangeOrderService = TestOrderService(),
            strategyExecutionRepository = TestStrategyExecutionRepository(),
        )
        val createdExecutors: MutableList<RememberingPriceStrategyExecutor> = mutableListOf()
        val strategyExecutorProvider = object : StrategyExecutorProvider by binanceStrategyExecutorProvider {
            override fun createStrategyExecutor(strategyParameters: StrategyParameters): StrategyExecutor {
                return binanceStrategyExecutorProvider.createStrategyExecutor(strategyParameters)
                    .rememberingPrice()
                    .also { createdExecutors += it }
            }
        }
        val tested = ExchangeStrategyExecutorService(
            strategyExecutionRepository = strategyExecutionRepository,
            strategyExecutorProvider = strategyExecutorProvider
        )
        tested.addStrategyExecutor(strategy1Parameters)
        tested.addStrategyExecutor(strategy2Parameters)
        // when
        val priceUpdate = CurrencyPairWithPrice(currencyPair = currencyPair, price = BigDecimal.ONE)
        tested.onPriceUpdated(priceUpdate)
        tested.onPriceUpdated(priceUpdate.copy(price = BigDecimal.TEN))
        // then
        assertThat(createdExecutors).hasSize(2)
        assertThat(createdExecutors).allSatisfy {
            assertThat(it.lastPrices).containsOnly(BigDecimal.ONE, BigDecimal.TEN)
        }
    }

}
