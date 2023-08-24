package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.app.config.objectMapper
import autocoin.binance.bot.exchange.apikey.ApiKeyDto
import autocoin.binance.bot.strategy.PositionBuyOrdersForFlashCrashStrategy
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.executor.StrategyType
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import com.autocoin.exchangegateway.api.exchange.xchange.SupportedXchangeExchange.binance
import com.autocoin.exchangegateway.spi.exchange.order.OrderStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.*

class StrategyExecutionMutableSetTest {
    private val sampleStrategyExecutionsJson =
        File(
            StrategyExecutionMutableSetTest::class.java.getResource("/sample-strategy-executions.json").toURI()
        ).absolutePath


    private val expectedStrategyExecution = StrategyExecutionDto(
        parameters = StrategyParametersDto(
            userId = "sample-user-id-1",
            baseCurrencyCode = "BTC",
            counterCurrencyCode = "USDT",
            strategyType = StrategyType.POSITION_BUY_ORDERS_FOR_FLASH_CRASH,
            strategySpecificParameters = PositionBuyOrdersForFlashCrashStrategy.ParametersBuilder()
                .withCounterCurrencyAmountLimitForBuying(1500.0.toBigDecimal())
                .toSMap(),
            apiKey = ApiKeyDto(
                publicKey = "sample binance api key",
                secretKey = "sample binance secret key",
            )
        ),
        id = "sample-execution-id-1",
        exchangeName = binance.exchangeName,
        createTimeMillis = 3,

        orders = listOf(
            StrategyOrder(
                id = "sample-order-id-1",
                exchangeOrderId = "exchange-order-id-1",
                status = OrderStatus.NEW,
                price = 10000.toBigDecimal(),
                amount = 0.1.toBigDecimal(),
                amountFilled = 0.toBigDecimal(),
                baseCurrencyCode = "BTC",
                counterCurrencyCode = "USDT",
                createTimeMillis = 13,
                closeTimeMillis = null
            ),
            StrategyOrder(
                id = "sample-order-id-2",
                exchangeOrderId = "exchange-order-id-2",
                status = OrderStatus.FILLED,
                price = 11000.toBigDecimal(),
                amount = 0.2.toBigDecimal(),
                amountFilled = 0.2.toBigDecimal(),
                baseCurrencyCode = "BTC",
                counterCurrencyCode = "USDT",
                createTimeMillis = 12,
                closeTimeMillis = 15
            ),
        ),
    )

    @Test
    fun shouldGetByUserId() {
        val tempDir = Files.createTempDirectory(UUID.randomUUID().toString())
        File(sampleStrategyExecutionsJson).copyTo(tempDir.resolve("strategy-executions-1.json").toFile())

        val tested = StrategyExecutionFileBackedMutableSetBuilder(
            fileRepositoryDirectory = tempDir.toFile(),
            objectMapper = objectMapper,
        ).build()
            .logging(logPrefix = "test")
        // when
        tested.load()
        val found = tested.filter { it.userId == "sample-user-id-1" }
        // then
        assertThat(found).hasSize(1)
        assertThat(found[0].id).isEqualTo(expectedStrategyExecution.id)
    }

    @Test
    fun shouldAddStrategy() {
        val tempDir = Files.createTempDirectory(UUID.randomUUID().toString())

        val tested = StrategyExecutionFileBackedMutableSetBuilder(
            fileRepositoryDirectory = tempDir.toFile(),
            objectMapper = objectMapper,
        ).build()
        // when
        tested.add(expectedStrategyExecution)
        tested.save()
        // then
        assertThat(tested).containsOnly(expectedStrategyExecution)
    }

    @Test
    fun shouldDeleteStrategy() {
        val tempDir = Files.createTempDirectory(UUID.randomUUID().toString())

        val tested = StrategyExecutionFileBackedMutableSetBuilder(
            fileRepositoryDirectory = tempDir.toFile(),
            objectMapper = objectMapper,
        ).build()
        val anotherStrategyExecution = expectedStrategyExecution.copy(id = "sample-execution-id-2")
        // when
        StrategyExecutionFileBackedMutableSetBuilder(
            fileRepositoryDirectory = tempDir.toFile(),
            objectMapper = objectMapper,
        ).build()
            .apply {
                add(expectedStrategyExecution)
                save()
                add(anotherStrategyExecution)
                save()
                remove(expectedStrategyExecution)
                save()
            }
        tested.load()
        // then
        assertThat(tested).containsOnly(anotherStrategyExecution)
    }
}
