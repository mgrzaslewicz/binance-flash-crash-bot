package autocoin.binance.bot.exchange

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyBalance
import com.autocoin.exchangegateway.spi.exchange.wallet.WithdrawResult
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import java.math.BigDecimal
import java.util.*

class TestWalletService(val currencyAmountAvailable: Map<String, BigDecimal>) : WalletServiceGateway<ApiKeyId> {
    data class Withdrawal(
        val currencyCode: String,
        val amount: BigDecimal,
        val address: String,
        val transactionId: String,
    )

    val withdrawals = mutableListOf<Withdrawal>()

    override fun getCurrencyBalance(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
    ) = object : CurrencyBalance {
        override val currencyCode = currencyCode
        override val amountAvailable = currencyAmountAvailable[currencyCode] ?: BigDecimal.ZERO
        override val totalAmount = BigDecimal.ZERO
        override val amountInOrders = BigDecimal.ZERO
    }

    override fun getCurrencyBalances(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>
    ): List<CurrencyBalance> {
        TODO("Not yet implemented")
    }

    override fun withdraw(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
        currencyCode: String,
        amount: BigDecimal,
        address: String,
    ): WithdrawResult = object : WithdrawResult {
        override val transactionId = UUID.randomUUID().toString()
    }.also {
        withdrawals.add(
            Withdrawal(
                currencyCode = currencyCode,
                amount = amount,
                address = address,
                transactionId = it.transactionId,
            )
        )
    }
}
