package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.app.config.ExchangeName
import autocoin.binance.bot.app.config.objectMapper
import autocoin.binance.bot.strategy.execution.StrategyExecution
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

    @Test
    fun shouldLoadUserConfiguration() {
        val tempDir = Files.createTempDirectory(UUID.randomUUID().toString())
        File(exchangeKeysPath).copyTo(tempDir.resolve("strategy-executions-1.json").toFile())

        val tested = FileStrategyExecutionRepository(
            fileRepositoryDirectory = tempDir,
            objectMapper = objectMapper,
            fileKeyValueRepository = FileKeyValueRepository(),
        )
        assertThat(tested.getExecutions("sample-user-id-1")).containsOnly(
            StrategyExecution(
                id = "sample-execution-id-1",
                userId = "sample-user-id-1",
                exchangeName = ExchangeName.BINANCE,

                baseCurrencyCode = "BTC",
                counterCurrencyCode = "USDT",

                counterCurrencyAmountLimitForBuying = 1500.0.toBigDecimal(),
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
        )
    }
}
