package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.app.config.objectMapper
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import com.autocoin.exchangegateway.spi.keyvalue.FileKeyValueRepository
import org.assertj.core.util.Files

class TestStrategyExecutionMutableSet(
    decorated: FileBackedMutableSet<StrategyExecutionDto> = StrategyExecutionFileBackedMutableSet(
        fileKeyValueRepository = FileKeyValueRepository(),
        fileRepositoryDirectory = Files.newTemporaryFolder().toPath(),
        objectMapper = objectMapper,
    ).logging(logPrefix = "test"),
) : FileBackedMutableSet<StrategyExecutionDto> by decorated
