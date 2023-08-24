package autocoin.binance.bot.exchange.binance

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.api.exchange.currency.defaultCurrencyPairToXchange
import com.autocoin.exchangegateway.api.exchange.order.Order
import com.autocoin.exchangegateway.api.exchange.order.service.authorized.XchangeAuthorizedOrderService
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.order.OrderSide
import com.autocoin.exchangegateway.spi.exchange.order.OrderStatus
import com.autocoin.exchangegateway.spi.exchange.order.service.authorized.AuthorizedOrderService
import mu.KLogging
import org.knowm.xchange.binance.dto.trade.OrderType
import org.knowm.xchange.binance.service.BinanceTradeService
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Clock
import java.util.*
import java.util.function.Function
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair as SpiCurrencyPair
import org.knowm.xchange.binance.dto.trade.OrderSide as XchangeOrderSide

/*
public String placeMarketOrder(MarketOrder mo) throws IOException {
return placeOrder(OrderType.MARKET, mo, null, null, null, null, null);


  private String placeOrder(
OrderType type,
Order order,
BigDecimal limitPrice,
BigDecimal stopPrice,
BigDecimal quoteOrderQty,
Long trailingDelta,
TimeInForce tif)
throws IOException {
try {
Long recvWindow =
  (Long)
      exchange.getExchangeSpecification().getExchangeSpecificParametersItem("recvWindow");
BinanceNewOrder newOrder =
  newOrder(
      order.getCurrencyPair(), // not null
      BinanceAdapters.convert(order.getType()),
      type, // not null
      tif,
      order.getOriginalAmount(), // needs to be null
      quoteOrderQty, // TODO (BigDecimal)order.getExtraValue("quoteOrderQty")
      limitPrice,
      getClientOrderId(order),
      stopPrice,
      trailingDelta, // TODO (Long)order.getExtraValue("trailingDelta")
      null,
      null);
 */

class AddingBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService<ApiKeyId>(
    private val clock: Clock,
    private val decorated: XchangeAuthorizedOrderService<ApiKeyId>,
    private val currencyPairToXchange: Function<CurrencyPair, org.knowm.xchange.currency.CurrencyPair> = defaultCurrencyPairToXchange,
) : AuthorizedOrderService<ApiKeyId> by decorated {
    private companion object : KLogging()

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    override fun placeMarketBuyOrderWithCounterCurrencyAmount(
        currencyPair: SpiCurrencyPair,
        counterCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
    ): Order {
        val binanceTradeService = decorated.delegate as BinanceTradeService

        val order = binanceTradeService.newOrder(
            /*pair = */currencyPairToXchange.apply(currencyPair),
            /*side = */XchangeOrderSide.BUY,
            /*type = */OrderType.MARKET,
            /*timeInForce = */null,
            /*quantity = */null,
            /*quoteOrderQty = */counterCurrencyAmount,
            /*limitPrice =*/null,
            /*clientOrderId = */null,
            /*stopPrice = */null,
            /*trailingDelta = */null,
            /*icebergQty = */null,
            /*newOrderRespType =*/null,
        )
        var orderPrice = BigDecimal.ZERO
        try {
            // order.price from exchange is null, so calculate the price. Be safe and don't divide by zero in case of unexpected data from exchange
            orderPrice = counterCurrencyAmount.divide(order.origQty, mathContext)
        } catch (e: ArithmeticException) {
            logger.error(e) { "Failed to calculate order price for order $order. Nothing to worry about, default value=0 was used in created order" }
        }
        return Order(
            exchange = exchange,
            exchangeOrderId = order.orderId.toString(),
            side = OrderSide.BID_BUY,
            orderedAmount = order.origQty,
            filledAmount = order.executedQty,
            price = orderPrice,
            currencyPair = currencyPair,
            status = OrderStatus.NEW,
            receivedAtMillis = clock.millis(),
            exchangeTimestampMillis = null,
        )
    }

}


//    AddingBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService
class AddingTestBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService(
    private val clock: Clock,
    private val decorated: XchangeAuthorizedOrderService<ApiKeyId>,
    private val currencyPairToXchange: Function<CurrencyPair, org.knowm.xchange.currency.CurrencyPair> = defaultCurrencyPairToXchange,
) : AuthorizedOrderService<ApiKeyId> by decorated {
    private companion object : KLogging()

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    override fun placeMarketBuyOrderWithCounterCurrencyAmount(
        currencyPair: SpiCurrencyPair,
        counterCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
    ): Order {
        val binanceTradeService = decorated.delegate as BinanceTradeService

        binanceTradeService.testNewOrder(
            /*pair = */currencyPairToXchange.apply(currencyPair),
            /*side = */XchangeOrderSide.BUY,
            /*type = */OrderType.MARKET,
            /*timeInForce = */null,
            /*quantity = */null,
            /*quoteOrderQty = */counterCurrencyAmount,
            /*limitPrice =*/null,
            /*clientOrderId = */null,
            /*stopPrice = */null,
            /*trailingDelta = */null,
            /*icebergQty = */null
        )
        return Order(
            exchange = exchange,
            exchangeOrderId = "test-" + UUID.randomUUID().toString(),
            side = OrderSide.BID_BUY,
            orderedAmount = counterCurrencyAmount.divide(currentPrice, mathContext),
            filledAmount = BigDecimal.ZERO,
            price = currentPrice,
            currencyPair = currencyPair,
            status = OrderStatus.NEW,
            receivedAtMillis = clock.millis(),
            exchangeTimestampMillis = null,
        )

    }
}

fun XchangeAuthorizedOrderService<ApiKeyId>.addingBinanceMarketOrderWithCounterCurrencyAmountBehavior(clock: Clock) =
    AddingBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService(clock, this)

fun XchangeAuthorizedOrderService<ApiKeyId>.addingTestBinanceMarketOrderWithCounterCurrencyAmountBehavior(clock: Clock) =
    AddingTestBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService(clock, this)
