package autocoin.binance.bot.strategy.repository

import mu.KLogging

class LoggingExecutionRepository(private val decorated: StrategyExecutionRepository) : StrategyExecutionRepository {
    private companion object : KLogging()

    override fun getConfigurations(userId: String): List<StrategyExecution> {
        return try {
            val configurations = decorated.getConfigurations(userId)
            logger.info { "User $userId has ${configurations.size} configurations" }
            return configurations
        } catch (e: Exception) {
            logger.error(e) { "Could not get user $userId configuration" }
            emptyList()
        }
    }

    override fun getConfigurations(): List<StrategyExecution> {
        return try {
            val configurations = decorated.getConfigurations()
            logger.info { "There are ${configurations.size} configurations" }
            return configurations
        } catch (e: Exception) {
            logger.error(e) { "Could not get configurations" }
            emptyList()
        }
    }


}

fun StrategyExecutionRepository.logging() = LoggingExecutionRepository(this)
