package autocoin.binance.bot.strategy.parameters

import autocoin.binance.bot.exchange.apikey.ApiKeyDto
import autocoin.binance.bot.strategy.executor.StrategyType
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY as SERIALIZE_PRIVATE_FIELDS

interface WithStrategySpecificParameters {
    fun getParameter(parameterName: String): String?
}

@JsonAutoDetect(fieldVisibility = SERIALIZE_PRIVATE_FIELDS)
data class StrategyParametersDto(
    val userId: String,
    val strategyType: StrategyType,
    val baseCurrencyCode: String,
    val counterCurrencyCode: String,
    @JsonIgnore
    val currencyPair: CurrencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode),
    private val strategySpecificParameters: Map<String, String>,
    val apiKey: ApiKeyDto,
) : WithStrategySpecificParameters {
    override fun getParameter(parameterName: String) = strategySpecificParameters[parameterName]
}
