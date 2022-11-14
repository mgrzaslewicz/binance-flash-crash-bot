package autocoin.binance.bot.strategy.repository

import autocoin.binance.bot.keyvalue.FileKeyValueRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FileStrategyExecutionRepository(
    private val fileKeyValueRepository: FileKeyValueRepository,
    private val fileRepositoryDirectory: Path,
    private val objectMapper: ObjectMapper
) : StrategyExecutionRepository {
    private companion object : KLogging()


    private object ExchangeKeysType : TypeReference<List<StrategyExecution>>()

    private fun getAllStrategyConfigurations(): List<StrategyExecution> {
        val file = fileRepositoryDirectory.toFile()
        val content = fileKeyValueRepository.getLatestVersion(file, "strategies")
        return if (content != null) {
            try {
                objectMapper.readValue(content.value, ExchangeKeysType)
            } catch (e: Exception) {
                throw Exception("Could not deserialize strategies from ${content.file.absolutePathString()})", e)
            }
        } else {
            emptyList()
        }
    }

    override fun getConfigurations(userId: String) = getAllStrategyConfigurations().filter { it.userId == userId }

    override fun getConfigurations() = getAllStrategyConfigurations()

}
