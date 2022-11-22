package autocoin.binance.bot.exchange

import java.time.Clock
import java.time.Duration

interface PriceListener {
    fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice)
}

