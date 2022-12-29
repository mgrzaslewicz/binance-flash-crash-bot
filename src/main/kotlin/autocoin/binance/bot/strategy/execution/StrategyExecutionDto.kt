package autocoin.binance.bot.strategy.execution

import autocoin.binance.bot.exchange.apikey.ApiKeyDto
import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.strategy.execution.repository.StrategyOrder
import autocoin.binance.bot.strategy.executor.StrategyType
import autocoin.binance.bot.strategy.parameters.WithStrategySpecificParameters
import com.autocoin.exchangegateway.api.exchange.ApiKey
import com.autocoin.exchangegateway.api.exchange.ApiKeySupplier
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.fasterxml.jackson.annotation.JsonIgnore
import java.security.MessageDigest
import java.util.*

private fun String.md5() = MessageDigest.getInstance("MD5").digest(this.toByteArray()).joinToString("") { "%02x".format(it) }

data class StrategyExecutionDto(
    val id: String = UUID.randomUUID().toString(),
    val strategyType: StrategyType,
    val userId: String,
    val exchangeName: String,
    val apiKey: ApiKeyDto,

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
    val apiKeySupplier: ApiKeySupplier<ApiKeyId> = ApiKeySupplier(
        id = ApiKeyId(
            userId = userId,
            keyHash = apiKey.publicKey.md5() + ":" + apiKey.secretKey.md5(),
        ),
        supplier = {
            ApiKey(
                publicKey = apiKey.publicKey,
                secretKey = apiKey.secretKey,
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

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
