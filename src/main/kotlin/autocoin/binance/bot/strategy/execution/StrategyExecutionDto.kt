package autocoin.binance.bot.strategy.execution

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.exchange.apikey.md5
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.parameters.WithStrategySpecificParameters
import autocoin.binance.bot.strategy.parameters.StrategyParametersDto
import com.autocoin.exchangegateway.api.exchange.apikey.ApiKey
import com.autocoin.exchangegateway.api.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.api.exchange.xchange.ExchangeNames
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.ZonedDateTime
import java.util.*

data class StrategyExecutionDto(
    val parameters: StrategyParametersDto,
    val id: String = UUID.randomUUID().toString(),
    val exchangeName: String,
    val orders: List<StrategyOrder> = emptyList(),
    val createTimeMillis: Long,
) : WithStrategySpecificParameters by parameters {

    companion object {
        fun StrategyParametersDto.toStrategyExecution(): StrategyExecutionDto {
            return StrategyExecutionDto(
                exchangeName = ExchangeNames.binance.value,
                parameters = this,
                createTimeMillis = ZonedDateTime.now().toInstant().toEpochMilli(),
            )
        }

        fun StrategyParametersDto.matchesStrategyExecution(strategyExecution: StrategyExecutionDto): Boolean {
            return userId == strategyExecution.userId
                    && currencyPair == strategyExecution.currencyPair
                    && strategyType == strategyExecution.strategyType
        }

        fun StrategyParametersDto.toResumedStrategyExecution(strategyExecution: StrategyExecutionDto): StrategyExecutionDto {
            check(matchesStrategyExecution(strategyExecution)) { "Cannot resume non matching strategy execution" }
            return strategyExecution.copy(
                parameters = strategyExecution.parameters.copy(apiKey = strategyExecution.parameters.apiKey)
            )
        }
    }

    @JsonIgnore
    val userId = parameters.userId

    @JsonIgnore
    val strategyType = parameters.strategyType

    @JsonIgnore
    val currencyPair = parameters.currencyPair

    @JsonIgnore
    val numberOfOrders = orders.size

    @JsonIgnore
    val apiKeySupplier: ApiKeySupplier<ApiKeyId> = ApiKeySupplier(
        id = ApiKeyId(
            userId = parameters.userId,
            keyHash = parameters.apiKey.publicKey.md5() + ":" + parameters.apiKey.secretKey.md5(),
        ),
        supplier = {
            ApiKey(
                publicKey = parameters.apiKey.publicKey,
                secretKey = parameters.apiKey.secretKey,
            )
        }
    )

    @JsonIgnore
    val ordersByPriceDesc: List<StrategyOrder> = orders.sortedByDescending { it.price }

    @JsonIgnore
    val ordersByPriceAsc: List<StrategyOrder> = orders.sortedBy { it.price }

    @JsonIgnore
    val orderWithMaxPrice: StrategyOrder? = ordersByPriceDesc.firstOrNull()

    @JsonIgnore
    val orderWithMinPrice: StrategyOrder? = ordersByPriceDesc.lastOrNull()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StrategyExecutionDto

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
