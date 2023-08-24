package autocoin.binance.bot.exchange.wallet

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.Exchange
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
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String
    ): CurrencyBalance {
        logger.info { "[${exchange.exchangeName}] Going to getCurrencyBalance apiKey.id=${apiKey.id}, currencyCode=$currencyCode" }
        return decorated.getCurrencyBalance(
            exchange = exchange,
            apiKey = apiKey,
            currencyCode = currencyCode
        )
    }

    override fun getCurrencyBalances(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>
    ): List<CurrencyBalance> {
        logger.info { "[${exchange.exchangeName}] Going to getCurrencyBalances apiKey.id=${apiKey.id}" }
        return decorated.getCurrencyBalances(
            exchange = exchange,
            apiKey = apiKey
        )
    }

    override fun withdraw(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
        amount: BigDecimal,
        address: String
    ): WithdrawResult {
        logger.info { "[${exchange.exchangeName}] Going to withdraw apiKey.id=${apiKey.id}, currencyCode=$currencyCode, amount=${amount.toPlainString()}, address=$address" }
        return decorated.withdraw(
            exchange = exchange,
            apiKey = apiKey,
            currencyCode = currencyCode,
            amount = amount,
            address = address
        )
    }

}

fun WalletServiceGateway<ApiKeyId>.preLogging(): WalletServiceGateway<ApiKeyId> =
    PreLoggingWalletServiceGateway(this)
