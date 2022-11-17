package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.strategy.execution.StrategyExecution
import mu.KLogging

class LoggingStrategyExecutionRepository(private val decorated: StrategyExecutionRepository) : StrategyExecutionRepository {
    private companion object : KLogging()

    override fun getExecutionsByUserId(userId: String): List<StrategyExecution> {
        return try {
            val executions = decorated.getExecutionsByUserId(userId)
            logger.info { "User $userId has ${executions.size} strategy executions" }
            return executions
        } catch (e: Exception) {
            logger.error(e) { "Could not get user $userId configuration" }
            emptyList()
        }
    }

    override fun getExecutions(): List<StrategyExecution> {
        return try {
            val configurations = decorated.getExecutions()
            logger.info { "There are ${configurations.size} strategy executions" }
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

    override fun save(strategyExecutions: List<StrategyExecution>) {
        logger.info { "Saving ${strategyExecutions.size} strategy executions" }
        decorated.save(strategyExecutions)
    }

    override fun delete(strategyExecutions: List<StrategyExecution>) {
        try {
            logger.info { "Deleting ${strategyExecutions.size} strategy executions" }
            decorated.delete(strategyExecutions).also {
                logger.info { "Deleted ${strategyExecutions.size} strategy executions" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Could not delete strategy ${strategyExecutions.size} executions" }
            throw e
        }
    }


}

fun StrategyExecutionRepository.logging() = LoggingStrategyExecutionRepository(this)
