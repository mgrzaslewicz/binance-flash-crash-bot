package autocoin.binance.bot.strategy

import autocoin.binance.bot.TestConfig
import autocoin.binance.bot.exchange.CurrencyPairWithPrice
import autocoin.binance.bot.exchange.TestOrderService
import autocoin.binance.bot.strategy.execution.repository.TestStrategyExecutionMutableSet
import autocoin.binance.bot.strategy.executor.*
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.google.common.util.concurrent.MoreExecutors
import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class ExchangeStrategyExecutorsServiceTest {
    companion object : KLogging()


    private val currencyPair = CurrencyPair.of("A", "B")


    private val strategy1Parameters = TestConfig.samplePositionBuyLimitOrdersSampleStrategyParameters()
    private val strategy2Parameters = strategy1Parameters.copy(
        userId = "user-2"
    )

    @Test
    fun shouldNotAllowToAddSecondStrategyWithTheSameUserAndCurrencyPair() {
        // given
        val strategyExecutions = TestStrategyExecutionMutableSet()
        val tested = ExchangeStrategyExecutorService(
            strategyExecutions = strategyExecutions,
            strategyExecutorProvider = BinanceStrategyExecutorProvider(
                exchangeOrderService = TestOrderService(),
                strategyExecutions = strategyExecutions,
                javaExecutorService = MoreExecutors.newDirectExecutorService(),
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
        val strategyExecutions = TestStrategyExecutionMutableSet()
        val binanceStrategyExecutorProvider = BinanceStrategyExecutorProvider(
            exchangeOrderService = TestOrderService(),
            strategyExecutions = strategyExecutions,
            javaExecutorService = MoreExecutors.newDirectExecutorService(),
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
            strategyExecutions = strategyExecutions,
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
