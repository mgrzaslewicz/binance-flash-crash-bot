package autocoin.binance.bot.exchange.wallet

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.exchange.ratelimit.RateLimiterProvider
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyBalance
import com.autocoin.exchangegateway.spi.exchange.wallet.WithdrawResult
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import mu.KLogging
import java.math.BigDecimal

class RateLimitingWalletServiceGateway(
    private val decorated: WalletServiceGateway<ApiKeyId>,
    private val rateLimiterProvider: RateLimiterProvider,
) : WalletServiceGateway<ApiKeyId> by decorated {
    companion object : KLogging()

    override fun getCurrencyBalance(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String
    ): CurrencyBalance {
        val howManySecondsWaited = rateLimiterProvider(apiKey.id).acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[${exchangeName.value}, apiKey.id=${apiKey.id}, currencyCode=$currencyCode] Waited ${howManySecondsWaited * 1000} ms to acquire getCurrencyBalance permit" }
        }
        return decorated.getCurrencyBalance(
            exchangeName = exchangeName,
            apiKey = apiKey,
            currencyCode = currencyCode
        )
    }

    override fun withdraw(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
        amount: BigDecimal,
        address: String
    ): WithdrawResult {
        val howManySecondsWaited = rateLimiterProvider(apiKey.id).acquire()
        if (howManySecondsWaited > 0.0) {
            logger.info { "[${exchangeName.value}, apiKey.id=${apiKey.id}, currencyCode=$currencyCode, amount=${amount.toPlainString()}, address=$address] Waited ${howManySecondsWaited * 1000} ms to acquire withdraw permit" }
        }
        return decorated.withdraw(
            exchangeName = exchangeName,
            apiKey = apiKey,
            currencyCode = currencyCode,
            amount = amount,
            address = address
        )
    }
}

fun WalletServiceGateway<ApiKeyId>.rateLimiting(rateLimiterProvider: RateLimiterProvider) =
    RateLimitingWalletServiceGateway(decorated = this, rateLimiterProvider = rateLimiterProvider)
