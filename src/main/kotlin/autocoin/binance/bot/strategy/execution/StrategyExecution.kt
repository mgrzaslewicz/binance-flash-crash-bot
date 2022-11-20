package autocoin.binance.bot.strategy.execution

import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.executor.StrategyType
import autocoin.binance.bot.strategy.parameters.WithStrategySpecificParameters
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*


data class StrategyExecution(
    val id: String = UUID.randomUUID().toString(),
    val strategyType: StrategyType,
    val userId: String,
    val exchangeName: String,
    val exchangeApiKey: ExchangeKeyDto,

    val baseCurrencyCode: String,
    val counterCurrencyCode: String,

    override val strategySpecificParameters: Map<String, String>,

    val orders: List<StrategyOrder> = emptyList(),

    val createTimeMillis: Long,

    ) : WithStrategySpecificParameters {
    @JsonIgnore
    val currencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode)

    @JsonIgnore
    val numberOfOrders = orders.size

    @JsonIgnore
    val ordersByPriceDesc: List<StrategyOrder> = orders.sortedByDescending { it.price }

    @JsonIgnore
    val ordersByPriceAsc: List<StrategyOrder> = orders.sortedBy { it.price }

    @JsonIgnore
    val orderWithMaxPrice: StrategyOrder? = ordersByPriceDesc.firstOrNull()

    @JsonIgnore
    val orderWithMinPrice: StrategyOrder? = ordersByPriceDesc.lastOrNull()
}
