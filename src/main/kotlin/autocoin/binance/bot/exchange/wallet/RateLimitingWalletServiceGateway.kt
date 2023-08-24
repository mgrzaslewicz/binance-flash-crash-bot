package autocoin.binance.bot.exchange.wallet

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyBalance
import com.autocoin.exchangegateway.spi.exchange.wallet.WithdrawResult
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import com.autocoin.exchangegateway.spi.ratelimiter.RateLimiterProvider
import mu.KLogging
import java.math.BigDecimal

class RateLimitingWalletServiceGateway(
    private val decorated: WalletServiceGateway<ApiKeyId>,
    private val rateLimiterProvider: RateLimiterProvider<ApiKeyId>,
) : WalletServiceGateway<ApiKeyId> by decorated {
    companion object : KLogging()

    override fun getCurrencyBalance(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String
    ): CurrencyBalance {
        val howManySecondsWaited = rateLimiterProvider(apiKey.id).acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[${exchange.exchangeName}, apiKey.id=${apiKey.id}, currencyCode=$currencyCode] Waited ${howManySecondsWaited * 1000} ms to acquire getCurrencyBalance permit" }
        }
        return decorated.getCurrencyBalance(
            exchange = exchange,
            apiKey = apiKey,
            currencyCode = currencyCode
        )
    }

    override fun withdraw(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
        amount: BigDecimal,
        address: String
    ): WithdrawResult {
        val howManySecondsWaited = rateLimiterProvider(apiKey.id).acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[${exchange.exchangeName}, apiKey.id=${apiKey.id}, currencyCode=$currencyCode, amount=${amount.toPlainString()}, address=$address] Waited ${howManySecondsWaited * 1000} ms to acquire withdraw permit" }
        }
        return decorated.withdraw(
            exchange = exchange,
            apiKey = apiKey,
            currencyCode = currencyCode,
            amount = amount,
            address = address
        )
    }
}

fun WalletServiceGateway<ApiKeyId>.rateLimiting(rateLimiterProvider: RateLimiterProvider<ApiKeyId>) =
    RateLimitingWalletServiceGateway(decorated = this, rateLimiterProvider = rateLimiterProvider)
