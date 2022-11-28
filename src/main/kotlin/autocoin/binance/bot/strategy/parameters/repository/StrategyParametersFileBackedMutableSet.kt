package autocoin.binance.bot.strategy.parameters.repository

import autocoin.binance.bot.strategy.execution.repository.FileBackedMutableHashSet
import autocoin.binance.bot.strategy.parameters.StrategyParameters
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path
import java.util.function.Function

class StrategyParametersFileBackedMutableSet(
    private val objectMapper: ObjectMapper,
    fileKeyValueRepository: FileKeyValueRepository,
    fileRepositoryDirectory: Path,
) : FileBackedMutableHashSet<StrategyParameters>(
    serializer = Function { strategyParameters ->
        objectMapper.writeValueAsString(strategyParameters)
    },
    deserializer = Function { strategyParametersJson ->
        objectMapper.readValue(strategyParametersJson, StrategyParametersType)
    },
    valueKey = "strategy-parameters",
    fileKeyValueRepository = fileKeyValueRepository,
    fileRepositoryDirectory = fileRepositoryDirectory,
) {
    private companion object {
        private object StrategyParametersType : TypeReference<Set<StrategyParameters>>()
    }
}

