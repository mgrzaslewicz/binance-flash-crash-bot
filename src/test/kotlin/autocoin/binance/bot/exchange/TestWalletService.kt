package autocoin.binance.bot.exchange

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyBalance
import com.autocoin.exchangegateway.spi.exchange.wallet.WithdrawResult
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import java.math.BigDecimal

class TestWalletService : WalletServiceGateway<ApiKeyId> {
    override fun getCurrencyBalance(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String
    ): CurrencyBalance {
        TODO("Not yet implemented")
    }

    override fun getCurrencyBalances(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>
    ): List<CurrencyBalance> {
        TODO("Not yet implemented")
    }

    override fun withdraw(
        exchangeName: ExchangeName,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
        amount: BigDecimal,
        address: String
    ): WithdrawResult {
        TODO("Not yet implemented")
    }
}
