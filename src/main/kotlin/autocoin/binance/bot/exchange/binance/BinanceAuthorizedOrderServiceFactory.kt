package autocoin.binance.bot.exchange.binance

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.api.exchange.order.service.authorized.XchangeAuthorizedOrderService
import com.autocoin.exchangegateway.api.exchange.order.service.authorized.XchangeAuthorizedOrderServiceFactory
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.order.service.authorized.AuthorizedOrderService
import com.autocoin.exchangegateway.spi.exchange.order.service.authorized.AuthorizedOrderServiceFactory
import java.util.function.Function

class BinanceAuthorizedOrderServiceFactory(
    private val decorated: XchangeAuthorizedOrderServiceFactory<ApiKeyId>,
    private val wrapper: Function<XchangeAuthorizedOrderService<ApiKeyId>, AuthorizedOrderService<ApiKeyId>>,
) : AuthorizedOrderServiceFactory<ApiKeyId> {
    override fun createAuthorizedOrderService(
        exchange: Exchange,
        apiKey: ApiKeySupplier<ApiKeyId>,
    ): AuthorizedOrderService<ApiKeyId> {
        val serviceToDecorate = decorated.createAuthorizedOrderService(exchange, apiKey)
        return wrapper.apply(serviceToDecorate)
    }
}
