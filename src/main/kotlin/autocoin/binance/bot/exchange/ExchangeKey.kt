package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto

fun exchangeKeyDto(apiKey: String, secretKey: String): ExchangeKeyDto {
    return ExchangeKeyDto(
        apiKey = apiKey,
        secretKey = secretKey,
        exchangeId = "does not matter",
        exchangeName = SupportedExchange.BINANCE.exchangeName,
        exchangeSpecificKeyParameters = emptyMap(),
        exchangeUserId = "does not matter",
        exchangeUserName = "does not matter",
        userName = null,
    )
}
