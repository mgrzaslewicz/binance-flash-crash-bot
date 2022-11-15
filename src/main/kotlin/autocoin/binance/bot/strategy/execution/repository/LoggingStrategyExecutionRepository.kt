package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.strategy.execution.StrategyExecution
import mu.KLogging

class LoggingStrategyExecutionRepository(private val decorated: StrategyExecutionRepository) : StrategyExecutionRepository {
    private companion object : KLogging()

    override fun getExecutions(userId: String): List<StrategyExecution> {
        return try {
            val configurations = decorated.getExecutions(userId)
            logger.info { "User $userId has ${configurations.size} configurations" }
            return configurations
        } catch (e: Exception) {
            logger.error(e) { "Could not get user $userId configuration" }
            emptyList()
        }
    }

    override fun getExecutions(): List<StrategyExecution> {
        return try {
            val configurations = decorated.getExecutions()
            logger.info { "There are ${configurations.size} configurations" }
            return configurations
        } catch (e: Exception) {
            logger.error(e) { "Could not get configurations" }
            emptyList()
        }
    }

    override fun save(strategyExecution: StrategyExecution) {
        try {
            logger.info { "Saving $strategyExecution" }
            decorated.save(strategyExecution).also {
                logger.info { "Saved $strategyExecution" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Could not save strategy execution $strategyExecution" }
            throw e
        }
    }


}

fun StrategyExecutionRepository.logging() = LoggingStrategyExecutionRepository(this)
