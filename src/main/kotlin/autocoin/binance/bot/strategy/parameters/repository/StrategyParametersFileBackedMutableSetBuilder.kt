package autocoin.binance.bot.strategy.parameters.repository

import autocoin.binance.bot.strategy.execution.repository.FileBackedMutableHashSet
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import com.autocoin.exchangegateway.api.keyvalue.FileKeyValueRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.time.Clock

class StrategyParametersFileBackedMutableSetBuilder(
    private var objectMapper: ObjectMapper,
    private var clock: Clock,
    private var fileRepositoryDirectory: File,
) {
    private companion object {
        private object StrategyParametersType : TypeReference<Set<StrategyParametersDto>>()
    }

    fun build(): FileBackedMutableHashSet<StrategyParametersDto> {
        return FileBackedMutableHashSet(
            valueKey = "strategy-parameters",
            keyValueRepository = FileKeyValueRepository.Builder<String, Set<StrategyParametersDto>>(
                directory = fileRepositoryDirectory,
                valueSerializer = { objectMapper.writeValueAsString(it) },
                valueDeserializer = { objectMapper.readValue(it, StrategyParametersType) },
            )
                .clock(clock)
                .build()
        )
    }

}

