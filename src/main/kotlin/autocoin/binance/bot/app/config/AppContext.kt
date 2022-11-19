package autocoin.binance.bot.app.config

import autocoin.binance.bot.eventbus.DefaultEventBus
import autocoin.binance.bot.exchange.*
import autocoin.binance.bot.httpclient.RequestLogInterceptor
import autocoin.binance.bot.strategy.ExchangeStrategyExecutorService
import autocoin.binance.bot.strategy.execution.repository.FileStrategyExecutionRepository
import autocoin.binance.bot.strategy.execution.repository.logging
import autocoin.binance.bot.strategy.executor.BinanceStrategyExecutorProvider
import autocoin.binance.bot.strategy.parameters.repository.FileStrategyParametersRepository
import autocoin.binance.bot.user.repository.FileUserRepository
import autocoin.binance.bot.user.repository.logging
import automate.profit.autocoin.exchange.CachingXchangeProvider
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.XchangeFactoryWrapper
import automate.profit.autocoin.exchange.XchangeSpecificationApiKeyAssigner
import automate.profit.autocoin.exchange.apikey.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.DemoOrderCreator
import automate.profit.autocoin.exchange.order.XchangeOrderService
import automate.profit.autocoin.exchange.peruser.ExchangeSpecificationVerifier
import automate.profit.autocoin.exchange.peruser.UserExchangeServicesFactory
import automate.profit.autocoin.exchange.peruser.XchangeUserExchangeServicesFactory
import automate.profit.autocoin.exchange.ratelimiter.ExchangeRateLimiters
import automate.profit.autocoin.exchange.wallet.ExchangeCurrencyPairsInWalletService
import automate.profit.autocoin.exchange.wallet.ExchangeWalletService
import automate.profit.autocoin.exchange.wallet.XchangeExchangeWalletService
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import com.binance.api.client.BinanceApiClientFactory
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import mu.KLogging
import okhttp3.OkHttpClient
import org.knowm.xchange.ExchangeFactory
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors.newSingleThreadScheduledExecutor
import java.util.concurrent.TimeUnit

class CurrencyPairDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        return CurrencyPair.of(key)
    }
}


val objectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(
        SimpleModule()
            .addKeyDeserializer(CurrencyPair::class.java, CurrencyPairDeserializer())
    )

class AppContext(private val appConfig: AppConfig) {
    companion object : KLogging()

    val clock = Clock.systemDefaultZone()

    val httpClient = OkHttpClient().newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(RequestLogInterceptor())
        .build()

    val webSocketClient = OkHttpClient().newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .addInterceptor(RequestLogInterceptor())
        .build()


    val eventBus = DefaultEventBus()

    val binancePriceStream = BinancePriceStream(
        eventBus = eventBus,
        binanceApiWebSocketClient = BinanceApiClientFactory.newInstance().newWebSocketClient(),
        clock = clock,
    )

    val priceWebSocketConnectionKeeper = PriceWebSocketConnectionKeeper(
        clock = clock,
        binancePriceStream = binancePriceStream,
        scheduledExecutor = newSingleThreadScheduledExecutor(),
    )


    val fileKeyValueRepository = FileKeyValueRepository()

    val strategyParametersRepository = FileStrategyParametersRepository(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory,
        objectMapper = objectMapper,
        fileKeyValueRepository = fileKeyValueRepository,
    )

    val strategyExecutionRepository = FileStrategyExecutionRepository(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory,
        objectMapper = objectMapper,
        fileKeyValueRepository = fileKeyValueRepository,
    ).logging()

    val exchangeRateLimiters = ExchangeRateLimiters(
        defaultPermitsPerDuration = 300L,
        defaultDuration = Duration.of(1L, ChronoUnit.MINUTES),
        permitAcquireTimeout = Duration.of(250L, ChronoUnit.MILLIS),
    )

    val userExchangeServicesFactory: UserExchangeServicesFactory = XchangeUserExchangeServicesFactory(
        serviceApiKeysProvider = object : ServiceApiKeysProvider {
            override fun getApiKeys(supportedExchange: SupportedExchange): ExchangeApiKey? {
                TODO("Not yet implemented")
            }
        },
        cachingXchangeProvider = CachingXchangeProvider(
            xchangeSpecificationApiKeyAssigner = XchangeSpecificationApiKeyAssigner(ExchangeSpecificationVerifier()),
            xchangeFactoryWrapper = XchangeFactoryWrapper(ExchangeFactory.INSTANCE),
        ),
        exchangeRateLimiters = exchangeRateLimiters,
        clock = clock,
    )
    val exchangeService = object : ExchangeService {
        override fun getExchangeIdByName(exchangeName: String): String {
            TODO("Not yet implemented")
        }

        override fun getExchangeNameById(exchangeId: String): String {
            TODO("Not yet implemented")
        }

        override fun getExchanges(): List<ExchangeDto> {
            TODO("Not yet implemented")
        }
    }
    val exchangeKeyService = object : ExchangeKeyService {
        override fun getExchangeKey(exchangeUserId: String, exchangeId: String): ExchangeKeyDto? {
            TODO("Not yet implemented")
        }

        override fun getExchangeKeys(): List<ExchangeKeyDto> {
            TODO("Not yet implemented")
        }

        override fun getExchangeKeys(exchangeUserId: String): List<ExchangeKeyDto> {
            TODO("Not yet implemented")
        }
    }

    val exchangeWalletService: ExchangeWalletService = if (appConfig.shouldMakeRealOrders) {
        XchangeExchangeWalletService(
            exchangeService = exchangeService,
            exchangeKeyService = exchangeKeyService,
            userExchangeServicesFactory = userExchangeServicesFactory,
        )
    } else {
        LoggingOnlyWalletService()
    }

    val exchangeCurrencyPairsInWalletService = object : ExchangeCurrencyPairsInWalletService {
        override fun generateFromWalletIfGivenEmpty(exchangeName: String, exchangeKey: ExchangeKeyDto, currencyPairs: List<CurrencyPair>): List<CurrencyPair> {
            TODO("Not yet implemented")
        }

        override fun generateFromWalletIfGivenEmpty(exchangeName: String, exchangeUserId: String, currencyPairs: List<CurrencyPair>): List<CurrencyPair> {
            TODO("Not yet implemented")
        }
    }

    val exchangeOrderService = if (appConfig.shouldMakeRealOrders) {
        logger.info { "Will make real orders" }
        XchangeOrderService(
            exchangeService = exchangeService,
            exchangeKeyService = exchangeKeyService,
            userExchangeServicesFactory = userExchangeServicesFactory,
            exchangeCurrencyPairsInWallet = exchangeCurrencyPairsInWalletService,
            demoOrderCreator = DemoOrderCreator(clock),
        )
            .measuringTime()
            .rateLimiting()

    } else {
        logger.warn { "Will NOT make real orders, just log them instead" }
        LoggingOnlyOrderService(clock = clock).rateLimiting()
    }


    val strategyExecutorProvider = BinanceStrategyExecutorProvider(
        exchangeWalletService = exchangeWalletService,
        exchangeOrderService = exchangeOrderService,
        strategyExecutionRepository = strategyExecutionRepository,
    )

    val strategyExecutionsService =
        ExchangeStrategyExecutorService(
            strategyExecutionRepository = strategyExecutionRepository,
            strategyExecutorProvider = strategyExecutorProvider,
        ).logging(minDelayBetweenLogs = Duration.ofSeconds(1), clock = clock)

    val userRepository = FileUserRepository(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory,
        objectMapper = objectMapper,
        fileKeyValueRepository = fileKeyValueRepository,
    ).logging()


}
