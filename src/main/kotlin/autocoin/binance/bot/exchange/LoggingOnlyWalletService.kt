package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.ExchangeCurrencyBalance
import automate.profit.autocoin.exchange.wallet.ExchangeWalletService
import automate.profit.autocoin.exchange.wallet.ExchangeWithErrorMessage
import mu.KLogging
import java.math.BigDecimal

class LoggingOnlyWalletService : ExchangeWalletService {
    companion object : KLogging()

    override fun getCurrencyBalance(exchangeName: String, exchangeKey: ExchangeKeyDto, currencyCode: String): ExchangeCurrencyBalance {
        logger.info { "[$exchangeName] Would get currencyBalance for currency=$currencyCode, key=$exchangeKey" }
        return ExchangeCurrencyBalance(
            currencyCode = currencyCode,
            amountAvailable = BigDecimal.ZERO,
            totalAmount = BigDecimal.ZERO,
            amountInOrders = BigDecimal.ZERO
        )
    }

    override fun getCurrencyBalance(exchangeName: String, exchangeUserId: String, currencyCode: String): ExchangeCurrencyBalance {
        TODO("Not yet implemented")
    }

    override fun getCurrencyBalances(exchangeName: String, exchangeKey: ExchangeKeyDto): List<ExchangeCurrencyBalance> {
        TODO("Not yet implemented")
    }

    override fun getCurrencyBalances(exchangeName: String, exchangeUserId: String): List<ExchangeCurrencyBalance> {
        TODO("Not yet implemented")
    }

    override fun getCurrencyBalancesForEveryExchange(exchangeUserId: String): Map<ExchangeWithErrorMessage, List<ExchangeCurrencyBalance>> {
        TODO("Not yet implemented")
    }

    override fun getCurrencyBalancesForEveryExchange(exchangeKeysGroupedByExchangeName: Map<String, List<ExchangeKeyDto>>): Map<ExchangeWithErrorMessage, List<ExchangeCurrencyBalance>> {
        TODO("Not yet implemented")
    }
}
