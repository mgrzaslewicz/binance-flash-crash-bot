package autocoin.binance.bot.strategy.execution.repository

import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import com.autocoin.exchangegateway.api.keyvalue.FileKeyValueRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.time.Clock

class StrategyExecutionFileBackedMutableSetBuilder(
    private var objectMapper: ObjectMapper,
    private var clock: Clock = Clock.systemDefaultZone(),
    private var fileRepositoryDirectory: File,
) {
    companion object {
        private object StrategyExecutionsType : TypeReference<Set<StrategyExecutionDto>>()
    }

    fun build(): FileBackedMutableHashSet<StrategyExecutionDto> {
        return FileBackedMutableHashSet(
            valueKey = "strategy-executions",
            keyValueRepository = FileKeyValueRepository.Builder<String, Set<StrategyExecutionDto>>(
                directory = fileRepositoryDirectory,
                valueSerializer = { objectMapper.writeValueAsString(it) },
                valueDeserializer = { objectMapper.readValue(it, StrategyExecutionsType) },
            )
                .clock(clock)
                .build()
        )
    }
}
