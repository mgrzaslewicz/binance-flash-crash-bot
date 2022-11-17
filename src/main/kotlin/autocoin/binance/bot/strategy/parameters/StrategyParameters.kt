package autocoin.binance.bot.strategy.parameters

import autocoin.binance.bot.app.config.ExchangeName
import autocoin.binance.bot.strategy.execution.StrategyExecution
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.time.ZonedDateTime

data class StrategyParameters(
    val userId: String,
    val baseCurrencyCode: String,
    val counterCurrencyCode: String,
    val counterCurrencyAmountLimitForBuying: BigDecimal,
    val numberOfBuyLimitOrdersToKeep: Int = 4,
    val exchangeApiKey: ExchangeKeyDto,
) {

    @JsonIgnore
    val currencyPair: CurrencyPair = CurrencyPair.Companion.of(baseCurrencyCode, counterCurrencyCode)
    fun toStrategyExecution(): StrategyExecution {
        return StrategyExecution(
            exchangeName = ExchangeName.BINANCE,
            userId = userId,
            baseCurrencyCode = currencyPair.base,
            counterCurrencyCode = currencyPair.counter,
            counterCurrencyAmountLimitForBuying = counterCurrencyAmountLimitForBuying,
            createTimeMillis = ZonedDateTime.now().toInstant().toEpochMilli(),
            exchangeApiKey = exchangeApiKey,
            numberOfBuyLimitOrdersToKeep = numberOfBuyLimitOrdersToKeep,
        )
    }

    fun matchesStrategyExecution(strategyExecution: StrategyExecution): Boolean {
        return userId == strategyExecution.userId && currencyPair == strategyExecution.currencyPair
    }

    fun toResumedStrategyExecution(strategyExecution: StrategyExecution): StrategyExecution {
        check(matchesStrategyExecution(strategyExecution)) { "Cannot resume non matching strategy execution" }
        return strategyExecution.copy(
            counterCurrencyAmountLimitForBuying = strategyExecution.counterCurrencyAmountLimitForBuying,
            numberOfBuyLimitOrdersToKeep = strategyExecution.numberOfBuyLimitOrdersToKeep,
            exchangeApiKey = strategyExecution.exchangeApiKey,
        )
    }
}
