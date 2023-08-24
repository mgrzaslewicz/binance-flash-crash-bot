package autocoin.binance.bot.exchange.wallet

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyBalance
import com.autocoin.exchangegateway.spi.exchange.Exchange
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
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
    ): CurrencyBalance {
        logger.info { "[${exchange.exchangeName}] Would get currencyBalance for currency=$currencyCode, key.id=${apiKey.id}" }
        return CurrencyBalance(
            currencyCode = currencyCode,
            amountAvailable = BigDecimal.ZERO,
            totalAmount = BigDecimal.ZERO,
            amountInOrders = BigDecimal.ZERO
        )
    }

    override fun getCurrencyBalances(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
    ): List<SpiCurrencyBalance> {
        TODO("Not yet implemented")
    }

    override fun withdraw(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
        amount: BigDecimal,
        address: String
    ): WithdrawResult {
        logger.info { "[${exchange.exchangeName}] Would withdraw for currency=$currencyCode, key.id=${apiKey.id}, amount=${amount.toPlainString()}, address=$address" }
        return object : WithdrawResult {
            override val transactionId = UUID.randomUUID().toString()
        }
    }

}
