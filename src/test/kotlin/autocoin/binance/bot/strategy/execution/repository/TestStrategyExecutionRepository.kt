package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.app.config.objectMapper
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import org.assertj.core.util.Files

class TestStrategyExecutionRepository : FileStrategyExecutionRepository(
    fileKeyValueRepository = FileKeyValueRepository(),
    fileRepositoryDirectory = Files.newTemporaryFolder().toPath(),
    objectMapper = objectMapper,
)
