package autocoin.binance.bot.exchange

import autocoin.binance.bot.eventbus.EventBus
import autocoin.binance.bot.eventbus.EventType
import com.autocoin.exchangegateway.api.exchange.currency.defaultCurrencyPairToXchange
import com.autocoin.exchangegateway.api.exchange.currency.defaultXchangeCurrencyPairTransformer
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.binance.api.client.BinanceApiCallback
import com.binance.api.client.BinanceApiWebSocketClient
import com.binance.api.client.domain.event.AggTradeEvent
import mu.KLogging
import org.knowm.xchange.binance.BinanceAdapters
import java.io.Closeable
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import kotlin.system.measureTimeMillis

data class CurrencyPairWithPrice(
    val currencyPair: CurrencyPair,
    val price: BigDecimal,
)

val priceUpdatedEventType = object : EventType<CurrencyPairWithPrice> {
    override fun isAsync() = false
}

class BinancePriceStream(
    private val eventBus: EventBus,
    private val binanceApiWebSocketClient: BinanceApiWebSocketClient,
    private val clock: Clock,
    private val currencyPairToXchange: Function<CurrencyPair, org.knowm.xchange.currency.CurrencyPair> = defaultCurrencyPairToXchange,
    private val xchangeCurrencyPairTransformer: Function<org.knowm.xchange.currency.CurrencyPair, CurrencyPair> = defaultXchangeCurrencyPairTransformer,
) {
    private companion object : KLogging()

    private val subscribedCurrencyPairs = mutableSetOf<CurrencyPair>()

    private val binanceApiWebSocket: AtomicReference<Closeable> = AtomicReference()
    private var connectionStartedAt: Instant? = null
    private var websocketFailureCount = AtomicInteger()
    private var websocketReconnectCount = AtomicInteger()

    private fun CurrencyPair.toBinanceSymbol() = BinanceAdapters.toSymbol(currencyPairToXchange.apply(this)).lowercase()
    private fun String.toCurrencyPair() = BinanceAdapters.adaptSymbol(this).let { xchangeCurrencyPairTransformer.apply(it) }

    private val apiCallback = object : BinanceApiCallback<AggTradeEvent> {

        override fun onResponse(event: AggTradeEvent) {
            val currencyPair = event.symbol.toCurrencyPair()
            eventBus.publish(
                eventType = priceUpdatedEventType,
                event = CurrencyPairWithPrice(price = event.price.toBigDecimal(), currencyPair = currencyPair)
            )
        }

        override fun onFailure(cause: Throwable?) {
            val previousConnectionDuration = Duration.between(connectionStartedAt, clock.instant())
            logger.error(cause) { "Websocket connection failed. It lasted for $previousConnectionDuration" }
            websocketFailureCount.incrementAndGet()
            reconnect()
        }

    }

    fun listenForPriceUpdates(currencyPairs: Set<CurrencyPair>) {
        subscribedCurrencyPairs.clear()
        subscribedCurrencyPairs.addAll(currencyPairs)
        reconnect()
    }

    private fun closePreviousConnection() {
        try {
            if (binanceApiWebSocket.get() != null) {
                logger.info { "Closing websocket connection" }
                binanceApiWebSocket.get().close()
                logger.info { "Websocket connection closed" }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error during closing connection" }
        } finally {
            binanceApiWebSocket.set(null)
        }
    }

    fun reconnect() {
        closePreviousConnection()
        val binanceSymbols = subscribedCurrencyPairs.joinToString(",") { it.toBinanceSymbol() }

        logger.info { "Creating websocket connection" }
        val millisecondsToConnect = measureTimeMillis {
            binanceApiWebSocket.set(binanceApiWebSocketClient.onAggTradeEvent(binanceSymbols, apiCallback))
        }
        connectionStartedAt = clock.instant()
        websocketReconnectCount.incrementAndGet()
        logger.info { "Websocket connection created within ${millisecondsToConnect}ms" }
    }

    fun getWebsocketFailureCount() = websocketFailureCount.get()
    fun getWebsocketReconnectCount() = websocketReconnectCount.get()
    fun getWebsocketConnectionStartedTimestamp() = connectionStartedAt

    fun isConnected() = binanceApiWebSocket.get() != null
}
