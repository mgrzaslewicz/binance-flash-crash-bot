package autocoin.binance.bot.exchange

import automate.profit.autocoin.api.exchange.currency.CurrencyBalance
import automate.profit.autocoin.spi.exchange.ExchangeName
import automate.profit.autocoin.spi.exchange.ExchangeWithErrorMessage
import automate.profit.autocoin.spi.exchange.apikey.ApiKey
import automate.profit.autocoin.spi.exchange.wallet.gateway.WalletServiceGateway
import mu.KLogging
import java.math.BigDecimal
import java.util.function.Supplier

class LoggingOnlyWalletServiceGateway : WalletServiceGateway {
    companion object : KLogging()

    override fun getCurrencyBalance(exchangeName: ExchangeName, apiKey: Supplier<ApiKey>, currencyCode: String): CurrencyBalance {
        logger.info { "[$exchangeName] Would get currencyBalance for currency=$currencyCode, key=$apiKey" }
        return CurrencyBalance(
            currencyCode = currencyCode,
            amountAvailable = BigDecimal.ZERO,
            totalAmount = BigDecimal.ZERO,
            amountInOrders = BigDecimal.ZERO
        )
    }

    override fun getCurrencyBalances(exchangeName: ExchangeName, apiKey: Supplier<ApiKey>): List<automate.profit.autocoin.spi.exchange.currency.CurrencyBalance> {
        TODO("Not yet implemented")
    }

    override fun getCurrencyBalancesFor(apiKeysGroupedByExchange: Map<ExchangeName, List<Supplier<ApiKey>>>): Map<ExchangeWithErrorMessage, List<automate.profit.autocoin.spi.exchange.currency.CurrencyBalance>> {
        TODO("Not yet implemented")
    }

}
