package autocoin.binance.bot.exchange.wallet

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyBalance
import com.autocoin.exchangegateway.spi.exchange.wallet.WithdrawResult
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import mu.KLogging
import java.math.BigDecimal

class PreLoggingWalletServiceGateway(private val decorated: WalletServiceGateway<ApiKeyId>) :
    WalletServiceGateway<ApiKeyId> {
    companion object : KLogging()

    override fun getCurrencyBalance(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String
    ): CurrencyBalance {
        logger.info { "[${exchangeName.value}] Going to getCurrencyBalance apiKey.id=${apiKey.id}, currencyCode=$currencyCode" }
        return decorated.getCurrencyBalance(
            exchangeName = exchangeName,
            apiKey = apiKey,
            currencyCode = currencyCode
        )
    }

    override fun getCurrencyBalances(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>
    ): List<CurrencyBalance> {
        logger.info { "[${exchangeName.value}] Going to getCurrencyBalances apiKey.id=${apiKey.id}" }
        return decorated.getCurrencyBalances(
            exchangeName = exchangeName,
            apiKey = apiKey
        )
    }

    override fun withdraw(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
        amount: BigDecimal,
        address: String
    ): WithdrawResult {
        logger.info { "[${exchangeName.value}] Going to withdraw apiKey.id=${apiKey.id}, currencyCode=$currencyCode, amount=${amount.toPlainString()}, address=$address" }
        return decorated.withdraw(
            exchangeName = exchangeName,
            apiKey = apiKey,
            currencyCode = currencyCode,
            amount = amount,
            address = address
        )
    }

}

fun WalletServiceGateway<ApiKeyId>.preLogging(): WalletServiceGateway<ApiKeyId> =
    PreLoggingWalletServiceGateway(this)
