package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.app.config.objectMapper
import org.assertj.core.util.Files

class TestStrategyExecutionMutableSet {
    companion object {
        fun get() = StrategyExecutionFileBackedMutableSetBuilder(
            fileRepositoryDirectory = Files.newTemporaryFolder(),
            objectMapper = objectMapper,
        ).build()
            .logging(logPrefix = "test")
    }
}
