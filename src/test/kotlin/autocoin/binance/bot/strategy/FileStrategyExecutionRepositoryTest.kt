package autocoin.binance.bot.strategy

import autocoin.binance.bot.app.config.ExchangeName
import autocoin.binance.bot.app.config.objectMapper
import autocoin.binance.bot.strategy.repository.FileStrategyExecutionRepository
import autocoin.binance.bot.strategy.repository.StrategyExecution
import autocoin.binance.bot.keyvalue.FileKeyValueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.*

class FileStrategyExecutionRepositoryTest {
    private val exchangeKeysPath =
        File(FileStrategyExecutionRepositoryTest::class.java.getResource("/sample-strategy-configurations.json").toURI()).absolutePath

    @Test
    fun shouldLoadUserConfiguration() {
        val tempDir = Files.createTempDirectory(UUID.randomUUID().toString())
        File(exchangeKeysPath).copyTo(tempDir.resolve("strategies-1.json").toFile())

        val tested = FileStrategyExecutionRepository(
            fileRepositoryDirectory = tempDir,
            objectMapper = objectMapper,
            fileKeyValueRepository = FileKeyValueRepository(),
        )
        assertThat(tested.getConfigurations("sample-user-id-1")).containsOnly(
            StrategyExecution(
                id = "sample-configuration-id-1",
                userId = "sample-user-id-1",
                exchangeName = ExchangeName.BINANCE,
                apiKey = "sample binance api key",
                secretKey = "sample binance secret key",
            )
        )
    }
}
