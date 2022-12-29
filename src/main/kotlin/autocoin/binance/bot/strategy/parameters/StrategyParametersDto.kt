package autocoin.binance.bot.strategy.parameters

import autocoin.binance.bot.exchange.apikey.ApiKeyDto
import autocoin.binance.bot.strategy.execution.StrategyExecutionDto
import autocoin.binance.bot.strategy.executor.StrategyType
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.api.exchange.xchange.ExchangeNames.Companion.binance
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.ZonedDateTime

interface WithStrategySpecificParameters {
    val strategySpecificParameters: Map<String, String>
}


data class StrategyParametersDto(
    val userId: String,
    val strategyType: StrategyType,
    val baseCurrencyCode: String,
    val counterCurrencyCode: String,
    override val strategySpecificParameters: Map<String, String>,
    val apiKey: ApiKeyDto,
) : WithStrategySpecificParameters {

    @JsonIgnore
    val currencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode)
    fun toStrategyExecution(): StrategyExecutionDto {
        return StrategyExecutionDto(
            exchangeName = binance.value,
            userId = userId,
            baseCurrencyCode = currencyPair.base,
            counterCurrencyCode = currencyPair.counter,
            createTimeMillis = ZonedDateTime.now().toInstant().toEpochMilli(),
            apiKey = apiKey,
            strategyType = strategyType,
            strategySpecificParameters = strategySpecificParameters,
        )
    }

    fun matchesStrategyExecution(strategyExecution: StrategyExecutionDto): Boolean {
        return userId == strategyExecution.userId
                && currencyPair == strategyExecution.currencyPair
                && strategyType == strategyExecution.strategyType
    }

    fun toResumedStrategyExecution(strategyExecution: StrategyExecutionDto): StrategyExecutionDto {
        check(matchesStrategyExecution(strategyExecution)) { "Cannot resume non matching strategy execution" }
        return strategyExecution.copy(
            apiKey = strategyExecution.apiKey,
        )
    }
}
