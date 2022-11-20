package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.app.config.objectMapper
import autocoin.binance.bot.strategy.counterCurrencyAmountLimitForBuyingParameter
import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.executor.StrategyType
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.order.ExchangeOrderStatus
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.*

class FileStrategyExecutionRepositoryTest {
    private val exchangeKeysPath =
        File(FileStrategyExecutionRepositoryTest::class.java.getResource("/sample-strategy-executions.json").toURI()).absolutePath


    private val expectedStrategyExecution = StrategyExecution(
        id = "sample-execution-id-1",
        userId = "sample-user-id-1",
        exchangeName = SupportedExchange.BINANCE.exchangeName,

        baseCurrencyCode = "BTC",
        counterCurrencyCode = "USDT",

        strategyType = StrategyType.POSITION_BUY_ORDERS_FOR_FLASH_CRASH,
        strategySpecificParameters = mapOf(
            counterCurrencyAmountLimitForBuyingParameter to 1500.0.toBigDecimal().toPlainString(),
        ),
        createTimeMillis = 3,
        exchangeApiKey = ExchangeKeyDto(
            apiKey = "sample binance api key",
            secretKey = "sample binance secret key",
            exchangeId = "does not matter",
            exchangeName = SupportedExchange.BINANCE.exchangeName,
            exchangeSpecificKeyParameters = emptyMap(),
            exchangeUserId = "does not matter",
            exchangeUserName = "does not matter",
            userName = null,
        ),

        orders = listOf(
            StrategyOrder(
                id = "sample-order-id-1",
                exchangeOrderId = "exchange-order-id-1",
                status = ExchangeOrderStatus.NEW,
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
                status = ExchangeOrderStatus.FILLED,
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
        File(exchangeKeysPath).copyTo(tempDir.resolve("strategy-executions-1.json").toFile())

        val tested = FileStrategyExecutionRepository(
            fileRepositoryDirectory = tempDir,
            objectMapper = objectMapper,
            fileKeyValueRepository = FileKeyValueRepository(),
        )
        // when
        val found = tested.getExecutionsByUserId("sample-user-id-1")
        // then
        assertThat(found).hasSize(1)
        assertThat(found[0].id).isEqualTo(expectedStrategyExecution.id)
    }

    @Test
    fun shouldAddStrategy() {
        val tempDir = Files.createTempDirectory(UUID.randomUUID().toString())

        val tested = FileStrategyExecutionRepository(
            fileRepositoryDirectory = tempDir,
            objectMapper = objectMapper,
            fileKeyValueRepository = FileKeyValueRepository(),
        )
        // when
        tested.save(expectedStrategyExecution)
        // then
        assertThat(tested.getExecutions()).containsOnly(expectedStrategyExecution)
    }

    @Test
    fun shouldDeleteStrategy() {
        val tempDir = Files.createTempDirectory(UUID.randomUUID().toString())

        val tested = FileStrategyExecutionRepository(
            fileRepositoryDirectory = tempDir,
            objectMapper = objectMapper,
            fileKeyValueRepository = FileKeyValueRepository(),
        )
        // when
        tested.save(expectedStrategyExecution)
        val anotherStrategyExecution = expectedStrategyExecution.copy(id = "sample-execution-id-2")
        tested.save(anotherStrategyExecution)
        tested.delete(expectedStrategyExecution)
        // then
        assertThat(tested.getExecutions()).containsOnly(anotherStrategyExecution)
    }
}
