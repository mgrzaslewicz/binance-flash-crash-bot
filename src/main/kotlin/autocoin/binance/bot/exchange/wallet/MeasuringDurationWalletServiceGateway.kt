package autocoin.binance.bot.exchange.wallet

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.measurement.MeasuringDurationWalletServiceGateway
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.measurement.OnGetCurrencyBalanceMeasured
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.measurement.OnWithdrawMeasured
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Duration

private val logger = KotlinLogging.logger {}

fun WalletServiceGateway<ApiKeyId>.measuringDuration(logPrefix: String = "") = MeasuringDurationWalletServiceGateway(
    decorated = this,
    onGetCurrencyBalanceMeasuredHandlers = listOf(
        object : OnGetCurrencyBalanceMeasured<ApiKeyId> {
            override fun invoke(
                exchange: Exchange,
                apiKey: ApiKeySupplier<ApiKeyId>,
                currencyCode: String,
                duration: Duration,
            ) {
                logger.info { "[$logPrefix${exchange.exchangeName}, apiKey.id=${apiKey.id}] Get currency balance took ${duration.toMillis()} ms" }
            }
        }
    ),
    onWithdrawMeasuredHandlers = listOf(
        object : OnWithdrawMeasured<ApiKeyId> {
            override fun invoke(
                exchange: Exchange,
                apiKey: ApiKeySupplier<ApiKeyId>,
                currencyCode: String,
                amount: BigDecimal,
                address: String,
                duration: Duration
            ) {
                logger.info { "[$logPrefix${exchange.exchangeName}, apiKey.id=${apiKey.id}, currency=$currencyCode, amount=${amount.toPlainString()}, address=$address] Withdraw took ${duration.toMillis()} ms" }
            }

        }
    ),
)
