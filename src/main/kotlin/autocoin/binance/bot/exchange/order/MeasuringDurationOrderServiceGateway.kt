package autocoin.binance.bot.exchange.order

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.order.CancelOrderParams
import com.autocoin.exchangegateway.spi.exchange.order.Order
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGateway
import com.autocoin.exchangegateway.spi.exchange.order.gateway.measurement.MeasuringDurationOrderServiceGateway
import com.autocoin.exchangegateway.spi.exchange.order.gateway.measurement.OnCancelOrderMeasured
import com.autocoin.exchangegateway.spi.exchange.order.gateway.measurement.OnPlaceLimitBuyOrderMeasured
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Duration

private val logger = KotlinLogging.logger {}
fun OrderServiceGateway<ApiKeyId>.measuringDuration(logPrefix: String = "") = MeasuringDurationOrderServiceGateway(
    decorated = this,
    onCancelOrderMeasuredHandlers = listOf(object : OnCancelOrderMeasured<ApiKeyId> {
        override fun invoke(
            exchange: Exchange,
            apiKey: ApiKeySupplier<ApiKeyId>,
            cancelOrderParams: CancelOrderParams,
            result: Boolean,
            duration: Duration,
        ) {
            logger.info { "[$logPrefix${exchange.exchangeName}, apiKey.id=${apiKey.id}] Canceled order took ${duration.toMillis()} ms" }
        }
    }),
    onPlaceLimitBuyOrderMeasured = listOf(
        object : OnPlaceLimitBuyOrderMeasured<ApiKeyId> {
            override operator fun invoke(
                exchange: Exchange,
                apiKey: ApiKeySupplier<ApiKeyId>,
                currencyPair: CurrencyPair,
                buyPrice: BigDecimal,
                amount: BigDecimal,
                result: Order,
                duration: Duration,
            ) {
                logger.info { "[$logPrefix${exchange.exchangeName}, apiKey.id=${apiKey.id}] Placed limit buy order $result. Took ${duration.toMillis()} ms" }
            }
        }
    )
)
