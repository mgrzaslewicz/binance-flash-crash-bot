package autocoin.binance.bot.exchange.wallet

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyBalance
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.wallet.WithdrawResult
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import mu.KLogging
import java.math.BigDecimal
import java.util.*
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyBalance as SpiCurrencyBalance

class LoggingOnlyWalletServiceGateway : WalletServiceGateway<ApiKeyId> {
    companion object : KLogging()

    override fun getCurrencyBalance(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
    ): CurrencyBalance {
        logger.info { "[$exchangeName] Would get currencyBalance for currency=$currencyCode, key.id=${apiKey.id}" }
        return CurrencyBalance(
            currencyCode = currencyCode,
            amountAvailable = BigDecimal.ZERO,
            totalAmount = BigDecimal.ZERO,
            amountInOrders = BigDecimal.ZERO
        )
    }

    override fun getCurrencyBalances(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
    ): List<SpiCurrencyBalance> {
        TODO("Not yet implemented")
    }

    override fun withdraw(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
        amount: BigDecimal,
        address: String
    ): WithdrawResult {
        logger.info { "[$exchangeName] Would withdraw for currency=$currencyCode, key.id=${apiKey.id}, amount=${amount.toPlainString()}, address=$address" }
        return object : WithdrawResult {
            override val transactionId = UUID.randomUUID().toString()
        }
    }

}