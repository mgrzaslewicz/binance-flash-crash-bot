package autocoin.binance.bot.strategy.parameters

import autocoin.binance.bot.exchange.apikey.ApiKeyDto
import autocoin.binance.bot.strategy.executor.StrategyType
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.fasterxml.jackson.annotation.JsonIgnore

interface WithStrategySpecificParameters {
    val strategySpecificParameters: Map<String, String>
}

data class StrategyParametersDto(
    val userId: String,
    val strategyType: StrategyType,
    val baseCurrencyCode: String,
    val counterCurrencyCode: String,
    @JsonIgnore
    val currencyPair: CurrencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode),
    override val strategySpecificParameters: Map<String, String>,
    val apiKey: ApiKeyDto,
) : WithStrategySpecificParameters
