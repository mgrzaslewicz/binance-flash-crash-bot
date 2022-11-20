package autocoin.binance.bot.strategy.parameters

import autocoin.binance.bot.strategy.execution.StrategyExecution
import autocoin.binance.bot.strategy.executor.StrategyType
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.fasterxml.jackson.annotation.JsonIgnore
import lombok.With
import java.time.ZonedDateTime

interface WithStrategySpecificParameters {
    val strategySpecificParameters: Map<String, String>
}

data class StrategyParameters(
    val userId: String,
    val strategyType: StrategyType,
    val baseCurrencyCode: String,
    val counterCurrencyCode: String,
    override val strategySpecificParameters: Map<String, String>,
    val exchangeApiKey: ExchangeKeyDto,
): WithStrategySpecificParameters {

    @JsonIgnore
    val currencyPair: CurrencyPair = CurrencyPair.Companion.of(baseCurrencyCode, counterCurrencyCode)
    fun toStrategyExecution(): StrategyExecution {
        return StrategyExecution(
            exchangeName = SupportedExchange.BINANCE.exchangeName,
            userId = userId,
            baseCurrencyCode = currencyPair.base,
            counterCurrencyCode = currencyPair.counter,
            createTimeMillis = ZonedDateTime.now().toInstant().toEpochMilli(),
            exchangeApiKey = exchangeApiKey,
            strategyType = strategyType,
            strategySpecificParameters = strategySpecificParameters,
        )
    }

    fun matchesStrategyExecution(strategyExecution: StrategyExecution): Boolean {
        return userId == strategyExecution.userId
                && currencyPair == strategyExecution.currencyPair
                && strategyType == strategyExecution.strategyType
    }

    fun toResumedStrategyExecution(strategyExecution: StrategyExecution): StrategyExecution {
        check(matchesStrategyExecution(strategyExecution)) { "Cannot resume non matching strategy execution" }
        return strategyExecution.copy(
            exchangeApiKey = strategyExecution.exchangeApiKey,
        )
    }
}
