package autocoin.binance.bot.exchange

interface PriceListener {
    fun onPriceUpdated(currencyPairWithPrice: CurrencyPairWithPrice)
}

