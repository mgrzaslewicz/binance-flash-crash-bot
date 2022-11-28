package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.strategy.execution.StrategyExecution
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path
import java.util.function.Function


class StrategyExecutionFileBackedMutableSet(
    private val objectMapper: ObjectMapper,
    fileKeyValueRepository: FileKeyValueRepository,
    fileRepositoryDirectory: Path,
) : FileBackedMutableHashSet<StrategyExecution>(
    serializer = Function { executions ->
        objectMapper.writeValueAsString(executions)
    },
    deserializer = Function { executionsJson ->
        objectMapper.readValue(executionsJson, StrategyExecutionsType)
    },
    valueKey = "strategy-executions",
    fileKeyValueRepository = fileKeyValueRepository,
    fileRepositoryDirectory = fileRepositoryDirectory,
) {
    private companion object {
        private object StrategyExecutionsType : TypeReference<Set<StrategyExecution>>()
    }
}
