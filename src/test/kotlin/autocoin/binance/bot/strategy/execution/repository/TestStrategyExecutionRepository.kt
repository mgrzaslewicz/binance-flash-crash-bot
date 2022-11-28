package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.app.config.objectMapper
import autocoin.binance.bot.strategy.execution.StrategyExecution
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import org.assertj.core.util.Files

class TestStrategyExecutionMutableSet(
    decorated: FileBackedMutableSet<StrategyExecution> = StrategyExecutionFileBackedMutableSet(
        fileKeyValueRepository = FileKeyValueRepository(),
        fileRepositoryDirectory = Files.newTemporaryFolder().toPath(),
        objectMapper = objectMapper,
    ).logging(logPrefix = "test")
) : FileBackedMutableSet<StrategyExecution> by decorated
