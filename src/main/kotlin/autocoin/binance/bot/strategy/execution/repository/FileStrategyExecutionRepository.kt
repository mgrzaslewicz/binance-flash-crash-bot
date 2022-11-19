package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.strategy.execution.StrategyExecution
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

open class FileStrategyExecutionRepository(
    private val fileKeyValueRepository: FileKeyValueRepository,
    private val fileRepositoryDirectory: Path,
    private val objectMapper: ObjectMapper
) : StrategyExecutionRepository {
    private companion object : KLogging()

    private val repositoryDirectory: File = fileRepositoryDirectory.toFile()
    private val valueKey = "strategy-executions"

    private object StrategyExecutionsType : TypeReference<List<StrategyExecution>>()

    private fun getAllStrategyExecutions(): List<StrategyExecution> {
        val content = fileKeyValueRepository.getLatestVersion(directory = repositoryDirectory, key = valueKey)
        return if (content != null) {
            try {
                objectMapper.readValue(content.value, StrategyExecutionsType)
            } catch (e: Exception) {
                throw Exception("Could not deserialize strategies from ${content.file.absolutePathString()})", e)
            }
        } else {
            emptyList()
        }
    }

    override fun getExecutionsByUserId(userId: String) = getAllStrategyExecutions().filter { it.userId == userId }

    override fun getExecutions() = getAllStrategyExecutions()

    override fun save(strategyExecutions: List<StrategyExecution>) {
        val allStrategyExecutions = getAllStrategyExecutions()
        val newList = allStrategyExecutions.toMutableList()
        strategyExecutions.forEach { strategyExecution ->
            val existingIndex = newList.indexOfFirst { it.id == strategyExecution.id }
            if (existingIndex != -1) {
                newList.removeAt(existingIndex)
            }
            newList += strategyExecution
        }
        val value = objectMapper.writeValueAsString(newList)
        fileKeyValueRepository.saveNewVersion(directory = repositoryDirectory, key = valueKey, value = value)
    }

    override fun delete(strategyExecutions: List<StrategyExecution>) {
        val allStrategyExecutions = getAllStrategyExecutions()
        val newList = allStrategyExecutions.toMutableList()
        strategyExecutions.forEach { strategyExecution ->
            val existingIndex = newList.indexOfFirst { it.id == strategyExecution.id }
            if (existingIndex != -1) {
                newList.removeAt(existingIndex)
            }
        }
        fileKeyValueRepository.saveNewVersion(directory = repositoryDirectory, key = valueKey, value = objectMapper.writeValueAsString(newList))
    }
}
