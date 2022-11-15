package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.ExchangeCurrencyBalance
import automate.profit.autocoin.exchange.wallet.ExchangeWalletService
import automate.profit.autocoin.exchange.wallet.ExchangeWithErrorMessage

class TestWalletService : ExchangeWalletService {
    override fun getCurrencyBalance(exchangeName: String, exchangeUserId: String, currencyCode: String): ExchangeCurrencyBalance {
        TODO("Not yet implemented")
    }

    override fun getCurrencyBalances(exchangeName: String, exchangeKey: ExchangeKeyDto): List<ExchangeCurrencyBalance> {
        TODO("Not yet implemented")
    }

    override fun getCurrencyBalance(exchangeName: String, exchangeKey: ExchangeKeyDto, currencyCode: String): ExchangeCurrencyBalance {
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
