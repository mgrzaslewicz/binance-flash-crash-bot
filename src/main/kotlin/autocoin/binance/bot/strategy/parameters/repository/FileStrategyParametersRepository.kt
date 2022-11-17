package autocoin.binance.bot.strategy.parameters.repository

import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FileStrategyParametersRepository(
    private val fileKeyValueRepository: FileKeyValueRepository,
    private val fileRepositoryDirectory: Path,
    private val objectMapper: ObjectMapper
): StrategyParametersRepository {
    private val repositoryDirectory: File = fileRepositoryDirectory.toFile()
    private val valueKey = "strategy-parameters"
    private object StrategyParametersType : TypeReference<List<StrategyParameters>>()

    override fun getAll(): List<StrategyParameters> {
        val content = fileKeyValueRepository.getLatestVersion(directory = repositoryDirectory, key = valueKey)
        return if (content != null) {
            try {
                objectMapper.readValue(content.value, StrategyParametersType)
            } catch (e: Exception) {
                throw Exception("Could not deserialize strategy parameters from ${content.file.absolutePathString()})", e)
            }
        } else {
            emptyList()
        }
    }

}
