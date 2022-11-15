package autocoin.binance.bot.exchange

import autocoin.binance.bot.eventbus.EventBus
import autocoin.binance.bot.eventbus.EventType
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiWebSocketClient
import mu.KLogging
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

data class CurrencyPairWithPrice(
    val currencyPair: CurrencyPair,
    val price: BigDecimal
)

val priceUpdatedEventType = object : EventType<CurrencyPairWithPrice> {
    override fun isAsync() = false
}

class BinanceTickerStream(
    private val eventBus: EventBus,
    private val binanceApiWebSocketClientProvider: Supplier<BinanceApiWebSocketClient> = Supplier { BinanceApiClientFactory.newInstance().newWebSocketClient() }
) {
    companion object : KLogging()

    private val binanceApiWebSocketClient: AtomicReference<BinanceApiWebSocketClient> = AtomicReference()
    private val currencyPairsAlreadyBeingListened = mutableSetOf<CurrencyPair>()

    fun CurrencyPair.toStringWithNoSeparator() = "${this.base}${this.counter}"

    fun listenForTicker(currencyPair: CurrencyPair) {
        if (binanceApiWebSocketClient.get() == null) {
            binanceApiWebSocketClient.set(binanceApiWebSocketClientProvider.get())
        }
        if (currencyPairsAlreadyBeingListened.contains(currencyPair)) {
            logger.warn { "Binance ticker stream is already listening for $currencyPair ticker" }
        } else {
            binanceApiWebSocketClient.get().onAggTradeEvent(currencyPair.toStringWithNoSeparator().lowercase()) { aggTradeEvent ->
                eventBus.publish(priceUpdatedEventType, CurrencyPairWithPrice(price = aggTradeEvent.price.toBigDecimal(), currencyPair = currencyPair))
            }
        }
    }
}
