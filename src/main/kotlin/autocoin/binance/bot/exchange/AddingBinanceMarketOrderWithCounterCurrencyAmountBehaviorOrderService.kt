package autocoin.binance.bot.exchange

import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.currency.toXchangeCurrencyPair
import automate.profit.autocoin.exchange.order.*
import automate.profit.autocoin.exchange.peruser.XchangeUserExchangeTradeService
import mu.KLogging
import org.knowm.xchange.binance.dto.trade.OrderSide
import org.knowm.xchange.binance.dto.trade.OrderType
import org.knowm.xchange.binance.service.BinanceTradeService
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Clock
import java.util.*

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

class AddingBinanceMarketOrderWithCounterCurrencyAmountBehaviorOrderService(
    private val clock: Clock,
    private val decorated: ExchangeOrderService
) : ExchangeOrderService by decorated {
    private companion object : KLogging()

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    override fun placeMarketBuyOrderWithCounterCurrencyAmount(
        exchangeName: String,
        exchangeKey: ExchangeKeyDto,
        baseCurrencyCode: String,
        counterCurrencyCode: String,
        counterCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
        isDemoOrder: Boolean
    ): ExchangeOrder {
        val xchangeOrderService = decorated as XchangeOrderService
        val tradeService = xchangeOrderService.getTradeService(exchangeName, exchangeKey) as XchangeUserExchangeTradeService
        val binanceTradeService = tradeService.wrapped as BinanceTradeService
        val currencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode)

        val order = binanceTradeService.newOrder(
            /*pair = */currencyPair.toXchangeCurrencyPair(),
            /*side = */OrderSide.BUY,
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
        return ExchangeOrder(
            exchangeName = exchangeName,
            orderId = order.orderId.toString(),
            type = ExchangeOrderType.BID_BUY,
            orderedAmount = order.origQty,
            filledAmount = order.executedQty,
            price = orderPrice,
            currencyPair = currencyPair,
            status = ExchangeOrderStatus.NEW,
            receivedAtMillis = clock.millis(),
            exchangeTimestampMillis = null,
        )

    }
}


class AddingTestBinanceMarketOrderWithCounterCurrencyAmountBehaviorOrderService(
    private val clock: Clock,
    private val decorated: ExchangeOrderService
) : ExchangeOrderService by decorated {
    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    override fun placeMarketBuyOrderWithCounterCurrencyAmount(
        exchangeName: String,
        exchangeKey: ExchangeKeyDto,
        baseCurrencyCode: String,
        counterCurrencyCode: String,
        counterCurrencyAmount: BigDecimal,
        currentPrice: BigDecimal,
        isDemoOrder: Boolean
    ): ExchangeOrder {
        val xchangeOrderService = decorated as XchangeOrderService
        val tradeService = xchangeOrderService.getTradeService(exchangeName, exchangeKey) as XchangeUserExchangeTradeService
        val binanceTradeService = tradeService.wrapped as BinanceTradeService
        val currencyPair = CurrencyPair.of(baseCurrencyCode, counterCurrencyCode)

        binanceTradeService.testNewOrder(
            /*pair = */currencyPair.toXchangeCurrencyPair(),
            /*side = */OrderSide.BUY,
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
        return ExchangeOrder(
            exchangeName = exchangeName,
            orderId = "test-" + UUID.randomUUID().toString(),
            type = ExchangeOrderType.BID_BUY,
            orderedAmount = counterCurrencyAmount.divide(currentPrice, mathContext),
            filledAmount = BigDecimal.ZERO,
            price = currentPrice,
            currencyPair = currencyPair,
            status = ExchangeOrderStatus.NEW,
            receivedAtMillis = clock.millis(),
            exchangeTimestampMillis = null,
        )

    }
}

fun ExchangeOrderService.addingBinanceMarketOrderWithCounterCurrencyAmountBehavior(clock: Clock) =
    AddingBinanceMarketOrderWithCounterCurrencyAmountBehaviorOrderService(clock, this)

fun ExchangeOrderService.addingTestBinanceMarketOrderWithCounterCurrencyAmountBehavior(clock: Clock) =
    AddingTestBinanceMarketOrderWithCounterCurrencyAmountBehaviorOrderService(clock, this)
